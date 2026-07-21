package com.payslipmax.pdfparser.insights

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
        val rawTax = computeOldSlabTax(netTaxable)
        val baseTax = applyOldRebate(netTaxable, rawTax, rules.sec87ARebateMaxIncomeOld)
        val cess = baseTax * CESS_RATE
        val totalTax = baseTax + cess
        val effectiveRate = if (grossIncome > 0) (totalTax / grossIncome) * 100.0 else 0.0

        return RegimeTaxDetail(
            regimeName = "OLD",
            grossIncome = grossIncome,
            standardDeduction = stdDed,
            totalDeductionsAndExemptions = totalDeductions,
            netTaxableIncome = netTaxable,
            baseTax = baseTax,
            cess = cess,
            totalTaxPayable = totalTax,
            effectiveTaxRatePct = effectiveRate,
        )
    }

    fun calculateNewRegimeTax(
        grossIncome: Double,
        fy: String = "2026-27",
    ): RegimeTaxDetail {
        val rules = TaxRuleKnowledgeBase.getRulesForFy(fy)
        val stdDed = rules.standardDeductionNew
        val netTaxable = maxOf(0.0, grossIncome - stdDed)

        val rawTax = computeNewSlabTax(netTaxable)
        val baseTax = applyNewRebate(netTaxable, rawTax, rules.sec87ARebateMaxIncomeNew)
        val cess = baseTax * CESS_RATE
        val totalTax = baseTax + cess
        val effectiveRate = if (grossIncome > 0) (totalTax / grossIncome) * 100.0 else 0.0

        return RegimeTaxDetail(
            regimeName = "NEW",
            grossIncome = grossIncome,
            standardDeduction = stdDed,
            totalDeductionsAndExemptions = stdDed,
            netTaxableIncome = netTaxable,
            baseTax = baseTax,
            cess = cess,
            totalTaxPayable = totalTax,
            effectiveTaxRatePct = effectiveRate,
        )
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

    private fun computeOldSlabTax(income: Double): Double {
        var tax = 0.0
        if (income > 250_000.0) {
            tax += (minOf(income, 500_000.0) - 250_000.0) * 0.05
        }
        if (income > 500_000.0) {
            tax += (minOf(income, 1_000_000.0) - 500_000.0) * 0.20
        }
        if (income > 1_000_000.0) {
            tax += (income - 1_000_000.0) * 0.30
        }
        return tax
    }

    private fun computeNewSlabTax(income: Double): Double {
        var tax = 0.0
        if (income > 300_000.0) {
            tax += (minOf(income, 700_000.0) - 300_000.0) * 0.05
        }
        if (income > 700_000.0) {
            tax += (minOf(income, 1_000_000.0) - 700_000.0) * 0.10
        }
        if (income > 1_000_000.0) {
            tax += (minOf(income, 1_200_000.0) - 1_000_000.0) * 0.15
        }
        if (income > 1_200_000.0) {
            tax += (minOf(income, 1_500_000.0) - 1_200_000.0) * 0.20
        }
        if (income > 1_500_000.0) {
            tax += (income - 1_500_000.0) * 0.30
        }
        return tax
    }

    private fun applyOldRebate(
        income: Double,
        tax: Double,
        maxRebateIncome: Double,
    ): Double {
        if (income <= maxRebateIncome) {
            return maxOf(0.0, tax - 12_500.0)
        }
        return tax
    }

    private fun applyNewRebate(
        income: Double,
        tax: Double,
        maxRebateIncome: Double,
    ): Double {
        if (income <= maxRebateIncome) {
            return 0.0
        }
        val diff = income - maxRebateIncome
        if (tax > diff) {
            return diff
        }
        return tax
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
