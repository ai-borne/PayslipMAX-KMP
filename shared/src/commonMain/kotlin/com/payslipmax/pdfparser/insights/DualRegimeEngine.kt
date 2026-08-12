package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.TaxRegime
import com.payslipmax.pdfparser.tax.RebateCalculator
import com.payslipmax.pdfparser.tax.SlabTaxCalculator
import com.payslipmax.pdfparser.tax.SurchargeCalculator
import com.payslipmax.pdfparser.tax.TaxRuleKnowledgeBase
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
 * Orchestrates old/new regime tax computation from the FY-versioned rule pack (SSOT:
 * [TaxRuleKnowledgeBase] and the `tax.rules` slab tables). Contains no hardcoded slabs, rebate
 * thresholds, or surcharge rates of its own (D2) -- every number here is looked up or delegated.
 */
object DualRegimeEngine {
    private const val CESS_RATE = 0.04

    fun calculateOldRegimeTax(
        grossIncome: Double,
        deductionsAndExemptions: Double = 0.0,
        fy: String = "2026-27",
    ): RegimeTaxDetail {
        val rules = TaxRuleKnowledgeBase.getRulesForFy(fy)
        val stdDed = rules.standardDeductionOld
        val totalDeductions = stdDed + deductionsAndExemptions
        val netTaxable = maxOf(0.0, grossIncome - totalDeductions)

        val rawTax = SlabTaxCalculator.computeTax(netTaxable, rules.oldRegimeSlabs)
        val baseTax =
            RebateCalculator.applyOldRegimeRebate(netTaxable, rawTax, rules.sec87ARebateMaxIncomeOld, rules.sec87ARebateCapOld)
        val surcharge = SurchargeCalculator.computeSurcharge(netTaxable, baseTax, rules.oldRegimeSlabs, isNewRegime = false)

        return buildDetail("OLD", grossIncome, stdDed, totalDeductions, netTaxable, baseTax, surcharge)
    }

    fun calculateNewRegimeTax(
        grossIncome: Double,
        fy: String = "2026-27",
    ): RegimeTaxDetail {
        val rules = TaxRuleKnowledgeBase.getRulesForFy(fy)
        val stdDed = rules.standardDeductionNew
        val netTaxable = maxOf(0.0, grossIncome - stdDed)

        val rawTax = SlabTaxCalculator.computeTax(netTaxable, rules.newRegimeSlabs)
        val baseTax =
            RebateCalculator.applyNewRegimeRebate(netTaxable, rawTax, rules.sec87ARebateMaxIncomeNew, rules.sec87ARebateCapNew)
        val surcharge = SurchargeCalculator.computeSurcharge(netTaxable, baseTax, rules.newRegimeSlabs, isNewRegime = true)

        return buildDetail("NEW", grossIncome, stdDed, stdDed, netTaxable, baseTax, surcharge)
    }

    fun compareRegimes(
        grossIncome: Double,
        oldRegimeDeductions: Double,
        fy: String = "2026-27",
    ): RegimeComparisonResult {
        val oldDetail = calculateOldRegimeTax(grossIncome, oldRegimeDeductions, fy)
        val newDetail = calculateNewRegimeTax(grossIncome, fy)

        val winner = if (newDetail.totalTaxPayable <= oldDetail.totalTaxPayable) "NEW" else "OLD"
        val savings = kotlin.math.abs(oldDetail.totalTaxPayable - newDetail.totalTaxPayable)
        val breakEven = computeBreakEvenDeduction(grossIncome, fy)

        return RegimeComparisonResult(
            financialYear = fy,
            oldRegime = oldDetail,
            newRegime = newDetail,
            winnerRegime = winner,
            annualSavings = savings,
            breakEvenDeduction = breakEven,
        )
    }

    /** Marginal rate for the given net taxable income under [regime]'s slabs for [fy] (D2: no hardcoded copy). */
    fun marginalRate(
        netTaxableIncome: Double,
        regime: TaxRegime,
        fy: String,
    ): Double {
        val rules = TaxRuleKnowledgeBase.getRulesForFy(fy)
        val slabs = if (regime == TaxRegime.NEW) rules.newRegimeSlabs else rules.oldRegimeSlabs
        return SlabTaxCalculator.marginalRate(netTaxableIncome, slabs)
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
        fy: String,
    ): Double {
        val newTax = calculateNewRegimeTax(gross, fy).totalTaxPayable
        var low = 0.0
        var high = gross
        for (i in 1..25) {
            val mid = (low + high) / 2.0
            val oldTax = calculateOldRegimeTax(gross, mid, fy).totalTaxPayable
            if (oldTax > newTax) {
                low = mid
            } else {
                high = mid
            }
        }
        return (low + high) / 2.0
    }
}
