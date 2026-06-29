package com.ssbmax.pdfparser.parser.registry

import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.TaxAndSavings
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.detection.GrammarFamily
import com.ssbmax.pdfparser.parser.detection.GrammarMatchResult
import com.ssbmax.pdfparser.parser.strategy.IGrammarHeaderStrategy
import com.ssbmax.pdfparser.parser.strategy.IGrammarPageStrategy
import com.ssbmax.pdfparser.parser.strategy.IGrammarStrategySet
import com.ssbmax.pdfparser.parser.strategy.IGrammarTableStrategy

object StubHeaderStrategy : IGrammarHeaderStrategy {
    override fun extractOfficer(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): Officer {
        return Officer(name = "OFFICER", accountNo = "16/000/000000X", pan = "AR*****90G")
    }
}

object StubTableStrategy : IGrammarTableStrategy {
    override fun extractRawEarnings(tokenized: TokenizedPayslip): Map<String, Double> = emptyMap()

    override fun extractRawDeductions(tokenized: TokenizedPayslip): Map<String, Double> = emptyMap()
}

object StubPageStrategy : IGrammarPageStrategy {
    override fun extractTaxAndSavings(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): TaxAndSavings? = null
}

object StubStrategySet : IGrammarStrategySet {
    override val headerStrategy: IGrammarHeaderStrategy = StubHeaderStrategy
    override val tableStrategy: IGrammarTableStrategy = StubTableStrategy
    override val pageStrategy: IGrammarPageStrategy = StubPageStrategy
}

object DefaultGrammarDescriptors {
    val LEGACY_STATEMENT =
        GrammarDescriptor(
            family = GrammarFamily.PCDA_LEGACY_STATEMENT,
            priority = 10,
            displayName = "Legacy Monolithic Statement",
            detectorMatcher = { tokenized -> matchLegacyStatement(tokenized) },
            strategySet = com.ssbmax.pdfparser.parser.strategy.legacy.LegacyStatementStrategySet,
        )

    val EARLY_DUAL_COL =
        GrammarDescriptor(
            family = GrammarFamily.PCDA_EARLY_DUAL_COL,
            priority = 20,
            displayName = "Early Dual-Column Statement",
            detectorMatcher = { tokenized -> matchEarlyDualCol(tokenized) },
            strategySet = com.ssbmax.pdfparser.parser.strategy.legacy.EarlyDualColStrategySet,
        )

    val TRANSITIONAL_7TH_CPC =
        GrammarDescriptor(
            family = GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC,
            priority = 30,
            displayName = "7th CPC Transitional Key-Value",
            detectorMatcher = { tokenized -> matchTransitional7thCpc(tokenized) },
            strategySet = com.ssbmax.pdfparser.parser.strategy.transitional.Transitional7thCpcStrategySet,
        )

    val MODERN_GRID =
        GrammarDescriptor(
            family = GrammarFamily.PCDA_MODERN_GRID,
            priority = 40,
            displayName = "Modern Reconstructed Spatial Grid",
            detectorMatcher = { tokenized -> matchModernGrid(tokenized) },
            strategySet = com.ssbmax.pdfparser.parser.strategy.modern.ModernGridStrategySet,
        )

    val EXTENDED_GRID =
        GrammarDescriptor(
            family = GrammarFamily.PCDA_EXTENDED_GRID,
            priority = 50,
            displayName = "Extended Multi-Container Spatial Grid",
            detectorMatcher = { tokenized -> matchExtendedGrid(tokenized) },
            strategySet = com.ssbmax.pdfparser.parser.strategy.modern.ExtendedGridStrategySet,
        )

    fun registerAll(registry: GrammarRegistry) {
        registry.register(LEGACY_STATEMENT)
        registry.register(EARLY_DUAL_COL)
        registry.register(TRANSITIONAL_7TH_CPC)
        registry.register(MODERN_GRID)
        registry.register(EXTENDED_GRID)
    }

    private fun matchLegacyStatement(tokenized: TokenizedPayslip): GrammarMatchResult {
        val text = tokenized.fullText
        val hasStatement = text.contains("STATEMENT OF ACCOUNT", ignoreCase = true)
        val hasCda = text.contains("CDA A/C", ignoreCase = true)
        return if (hasStatement && !hasCda) {
            GrammarMatchResult(true, listOf("Signature: STATEMENT OF ACCOUNT without CDA A/C"))
        } else {
            GrammarMatchResult(false, rejectedReasons = listOf("Missing legacy statement anchor or contains CDA A/C"))
        }
    }

    private fun matchEarlyDualCol(tokenized: TokenizedPayslip): GrammarMatchResult {
        val text = tokenized.fullText
        val hasCda = text.contains("CDA A/C", ignoreCase = true)
        val hasBandOrGrade = text.contains("Band Pay", ignoreCase = true) || text.contains("Grade Pay", ignoreCase = true)
        return if (hasCda && hasBandOrGrade) {
            GrammarMatchResult(true, listOf("Signature: Band Pay / Grade Pay"))
        } else {
            GrammarMatchResult(false, rejectedReasons = listOf("Missing Band Pay / Grade Pay anchor"))
        }
    }

    private fun matchTransitional7thCpc(tokenized: TokenizedPayslip): GrammarMatchResult {
        val text = tokenized.fullText
        val hasBasicPay = text.contains("Basic Pay", ignoreCase = true)
        val hasBpay = text.contains("BPAY", ignoreCase = true)
        return if (hasBasicPay && !hasBpay) {
            GrammarMatchResult(true, listOf("Signature: Basic Pay without BPAY"))
        } else {
            GrammarMatchResult(false, rejectedReasons = listOf("Missing Basic Pay anchor or contains BPAY"))
        }
    }

    private fun matchModernGrid(tokenized: TokenizedPayslip): GrammarMatchResult {
        val text = tokenized.fullText
        val hasBpay = text.contains("BPAY", ignoreCase = true)
        val hasGrossOrDo2 = text.contains("Gross Pay", ignoreCase = true) || text.contains("DO2 Details", ignoreCase = true)
        return if (hasBpay && hasGrossOrDo2) {
            GrammarMatchResult(true, listOf("Signature: BPAY and Gross Pay"))
        } else {
            GrammarMatchResult(false, rejectedReasons = listOf("Missing BPAY/Gross Pay"))
        }
    }

    private fun matchExtendedGrid(tokenized: TokenizedPayslip): GrammarMatchResult {
        val text = tokenized.fullText
        val hasBpay = text.contains("BPAY", ignoreCase = true)
        val hasArrearsContainer = text.contains("ARR-", ignoreCase = true) || text.contains("Extended Grid", ignoreCase = true)
        return if (hasBpay && hasArrearsContainer) {
            GrammarMatchResult(true, listOf("Signature: BPAY and Extended Arrears Container (ARR-)"))
        } else {
            GrammarMatchResult(false, rejectedReasons = listOf("Missing extended arrears container anchor"))
        }
    }
}
