package com.ssbmax.pdfparser.parser.strategy.legacy

import com.ssbmax.pdfparser.domain.TaxAndSavings
import com.ssbmax.pdfparser.parser.TokenText
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.parseTaxAndSavings
import com.ssbmax.pdfparser.parser.strategy.IGrammarPageStrategy

/**
 * Page strategy for historical legacy statement formats.
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object LegacyPageStrategy : IGrammarPageStrategy {
    override fun extractTaxAndSavings(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): TaxAndSavings? {
        val taxText = TokenText.readingOrder(tokenized.taxTokens)
        val dsopText = TokenText.readingOrder(tokenized.dsopTokens)
        return parseTaxAndSavings(taxText, dsopText, cleanedText)
    }
}
