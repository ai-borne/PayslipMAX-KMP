package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.TaxRegime
import com.payslipmax.pdfparser.tax.RebateCalculator
import com.payslipmax.pdfparser.tax.SlabTaxCalculator
import com.payslipmax.pdfparser.tax.SurchargeCalculator
import com.payslipmax.pdfparser.tax.TaxRuleKnowledgeBase
import com.payslipmax.pdfparser.tax.TaxRuleResolution
import com.payslipmax.pdfparser.tax.TaxYearRules
import kotlinx.serialization.Serializable

@Serializable
data class RegimeTaxDetail(
    val regimeName: String,
    val grossIncome: Double,
    val standardDeduction: Double,
    val totalDeductionsAndExemptions: Double,
    val netTaxableIncome: Double,
    val baseTax: Double,
    val surcharge: Double,
    val cess: Double,
    val totalTaxPayable: Double,
    val effectiveTaxRatePct: Double,
)

@Serializable
data class RegimeComparisonResult(
    val financialYear: String,
    val oldRegime: RegimeTaxDetail,
    val newRegime: RegimeTaxDetail,
    val winnerRegime: String,
    val annualSavings: Double,
    val breakEvenDeduction: Double,
)

/**
 * ADR-2: every entry point below that depends on an FY's rule pack returns one of these instead of
 * silently substituting the nearest known FY's numbers for an FY the rule pack doesn't cover.
 */
sealed class RegimeTaxOutcome {
    data class Available(
        val detail: RegimeTaxDetail,
    ) : RegimeTaxOutcome()

    data class RulesUnavailable(
        val requestedFy: String,
        val nearestKnownFy: String,
    ) : RegimeTaxOutcome()
}

sealed class RegimeComparisonOutcome {
    data class Available(
        val result: RegimeComparisonResult,
    ) : RegimeComparisonOutcome()

    data class RulesUnavailable(
        val requestedFy: String,
        val nearestKnownFy: String,
    ) : RegimeComparisonOutcome()
}

/**
 * Orchestrates old/new regime tax computation from the FY-versioned rule pack (SSOT:
 * [TaxRuleKnowledgeBase] and the `tax.rules` slab tables). Contains no hardcoded slabs, rebate
 * thresholds, or surcharge rates of its own (D2) -- every number here is looked up or delegated.
 * Every public entry point resolves its FY via [TaxRuleKnowledgeBase.resolve] (ADR-2), never the
 * silently-falls-back-to-nearest-FY `getRulesForFy` -- callers must handle an unresolvable FY
 * explicitly instead of receiving a wrong number for the wrong year.
 */
object DualRegimeEngine {
    private const val CESS_RATE = 0.04

    fun calculateOldRegimeTax(
        grossIncome: Double,
        deductionsAndExemptions: Double = 0.0,
        fy: String = "2026-27",
    ): RegimeTaxOutcome =
        withResolvedRules(fy) { rules -> oldRegimeDetail(grossIncome, deductionsAndExemptions, rules) }

    fun calculateNewRegimeTax(
        grossIncome: Double,
        fy: String = "2026-27",
    ): RegimeTaxOutcome = withResolvedRules(fy) { rules -> newRegimeDetail(grossIncome, rules) }

    fun compareRegimes(
        grossIncome: Double,
        oldRegimeDeductions: Double,
        fy: String = "2026-27",
    ): RegimeComparisonOutcome =
        when (val resolution = TaxRuleKnowledgeBase.resolve(fy)) {
            is TaxRuleResolution.OutOfRange ->
                RegimeComparisonOutcome.RulesUnavailable(resolution.requestedFy, resolution.nearestKnownFy)
            is TaxRuleResolution.Resolved -> {
                val rules = resolution.rules
                val oldDetail = oldRegimeDetail(grossIncome, oldRegimeDeductions, rules)
                val newDetail = newRegimeDetail(grossIncome, rules)
                val winner = if (newDetail.totalTaxPayable <= oldDetail.totalTaxPayable) "NEW" else "OLD"
                val savings = kotlin.math.abs(oldDetail.totalTaxPayable - newDetail.totalTaxPayable)
                val breakEven = computeBreakEvenDeduction(grossIncome, newDetail.totalTaxPayable, rules)

                RegimeComparisonOutcome.Available(
                    RegimeComparisonResult(
                        financialYear = fy,
                        oldRegime = oldDetail,
                        newRegime = newDetail,
                        winnerRegime = winner,
                        annualSavings = savings,
                        breakEvenDeduction = breakEven,
                    ),
                )
            }
        }

    /**
     * Marginal rate for [netTaxableIncome] under [regime]'s slabs for [fy] (D2: no hardcoded copy).
     * Null when [fy] has no resolvable rule pack (ADR-2) -- never a silently-wrong nearest-FY rate.
     */
    fun marginalRate(
        netTaxableIncome: Double,
        regime: TaxRegime,
        fy: String,
    ): Double? =
        when (val resolution = TaxRuleKnowledgeBase.resolve(fy)) {
            is TaxRuleResolution.OutOfRange -> null
            is TaxRuleResolution.Resolved -> {
                val slabs = if (regime == TaxRegime.NEW) resolution.rules.newRegimeSlabs else resolution.rules.oldRegimeSlabs
                SlabTaxCalculator.marginalRate(netTaxableIncome, slabs)
            }
        }

    private inline fun withResolvedRules(
        fy: String,
        compute: (TaxYearRules) -> RegimeTaxDetail,
    ): RegimeTaxOutcome =
        when (val resolution = TaxRuleKnowledgeBase.resolve(fy)) {
            is TaxRuleResolution.OutOfRange ->
                RegimeTaxOutcome.RulesUnavailable(resolution.requestedFy, resolution.nearestKnownFy)
            is TaxRuleResolution.Resolved -> RegimeTaxOutcome.Available(compute(resolution.rules))
        }

    private fun oldRegimeDetail(
        grossIncome: Double,
        deductionsAndExemptions: Double,
        rules: TaxYearRules,
    ): RegimeTaxDetail {
        val stdDed = rules.standardDeductionOld
        val totalDeductions = stdDed + deductionsAndExemptions
        val netTaxable = maxOf(0.0, grossIncome - totalDeductions)

        val rawTax = SlabTaxCalculator.computeTax(netTaxable, rules.oldRegimeSlabs)
        val baseTax =
            RebateCalculator.applyOldRegimeRebate(netTaxable, rawTax, rules.sec87ARebateMaxIncomeOld, rules.sec87ARebateCapOld)
        val surcharge = SurchargeCalculator.computeSurcharge(netTaxable, baseTax, rules.oldRegimeSlabs, isNewRegime = false)

        return buildDetail("OLD", grossIncome, stdDed, totalDeductions, netTaxable, baseTax, surcharge)
    }

    private fun newRegimeDetail(
        grossIncome: Double,
        rules: TaxYearRules,
    ): RegimeTaxDetail {
        val stdDed = rules.standardDeductionNew
        val netTaxable = maxOf(0.0, grossIncome - stdDed)

        val rawTax = SlabTaxCalculator.computeTax(netTaxable, rules.newRegimeSlabs)
        val baseTax =
            RebateCalculator.applyNewRegimeRebate(netTaxable, rawTax, rules.sec87ARebateMaxIncomeNew, rules.sec87ARebateCapNew)
        val surcharge = SurchargeCalculator.computeSurcharge(netTaxable, baseTax, rules.newRegimeSlabs, isNewRegime = true)

        return buildDetail("NEW", grossIncome, stdDed, stdDed, netTaxable, baseTax, surcharge)
    }

    private fun buildDetail(
        regimeName: String,
        grossIncome: Double,
        standardDeduction: Double,
        totalDeductions: Double,
        netTaxable: Double,
        baseTax: Double,
        surcharge: Double,
    ): RegimeTaxDetail {
        val cess = (baseTax + surcharge) * CESS_RATE
        val totalTax = baseTax + surcharge + cess
        val effectiveRate = if (grossIncome > 0) (totalTax / grossIncome) * 100.0 else 0.0

        return RegimeTaxDetail(
            regimeName = regimeName,
            grossIncome = grossIncome,
            standardDeduction = standardDeduction,
            totalDeductionsAndExemptions = totalDeductions,
            netTaxableIncome = netTaxable,
            baseTax = baseTax,
            surcharge = surcharge,
            cess = cess,
            totalTaxPayable = totalTax,
            effectiveTaxRatePct = effectiveRate,
        )
    }

    private fun computeBreakEvenDeduction(
        gross: Double,
        newTax: Double,
        rules: TaxYearRules,
    ): Double {
        var low = 0.0
        var high = gross
        for (i in 1..25) {
            val mid = (low + high) / 2.0
            val oldTax = oldRegimeDetail(gross, mid, rules).totalTaxPayable
            if (oldTax > newTax) {
                low = mid
            } else {
                high = mid
            }
        }
        return (low + high) / 2.0
    }
}
