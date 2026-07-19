package com.payslipmax.pdfparser.insights

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
    private const val STD_DED_OLD = 50_000.0
    private const val STD_DED_NEW_FY24 = 50_000.0
    private const val STD_DED_NEW_FY25 = 75_000.0

    fun calculateOldRegimeTax(
        grossIncome: Double,
        deductionsAndExemptions: Double = 0.0,
    ): RegimeTaxDetail {
        val totalDeductions = STD_DED_OLD + deductionsAndExemptions
        val netTaxable = maxOf(0.0, grossIncome - totalDeductions)
        val rawTax = computeOldSlabTax(netTaxable)
        val baseTax = applyOldRebate(netTaxable, rawTax)
        val cess = baseTax * CESS_RATE
        val totalTax = baseTax + cess
        val effectiveRate = if (grossIncome > 0) (totalTax / grossIncome) * 100.0 else 0.0

        return RegimeTaxDetail(
            regimeName = "OLD",
            grossIncome = grossIncome,
            standardDeduction = STD_DED_OLD,
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
        fy: String = "2024-25",
    ): RegimeTaxDetail {
        val stdDed = if (fy == "2025-26") STD_DED_NEW_FY25 else STD_DED_NEW_FY24
        val netTaxable = maxOf(0.0, grossIncome - stdDed)
        val isFy2526 = (fy == "2025-26")

        val rawTax = if (isFy2526) computeNewSlabTaxFy2526(netTaxable) else computeNewSlabTaxFy2425(netTaxable)
        val baseTax = if (isFy2526) applyNewRebateFy2526(netTaxable, rawTax) else applyNewRebateFy2425(netTaxable, rawTax)
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
        fy: String = "2024-25",
    ): RegimeComparisonResult {
        val oldDetail = calculateOldRegimeTax(grossIncome, oldRegimeDeductions)
        val newDetail = calculateNewRegimeTax(grossIncome, fy)

        val winner = if (oldDetail.totalTaxPayable <= newDetail.totalTaxPayable) "OLD" else "NEW"
        val savings = kotlin.math.abs(oldDetail.totalTaxPayable - newDetail.totalTaxPayable)
        val breakEven = computeBreakEvenDeduction(grossIncome, newDetail.totalTaxPayable)

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
        if (income > 10_00_000) tax += (income - 10_00_000) * 0.30
        if (income > 5_00_000) tax += minOf(5_00_000.0, income - 5_00_000) * 0.20
        if (income > 2_50_000) tax += minOf(2_50_000.0, income - 2_50_000) * 0.05
        return tax
    }

    private fun applyOldRebate(
        income: Double,
        tax: Double,
    ): Double {
        if (income <= 5_00_000) {
            val rebate = minOf(12_500.0, tax)
            return maxOf(0.0, tax - rebate)
        }
        return tax
    }

    private fun computeNewSlabTaxFy2425(income: Double): Double {
        var tax = 0.0
        if (income > 15_00_000) tax += (income - 15_00_000) * 0.30
        if (income > 12_00_000) tax += minOf(3_00_000.0, income - 12_00_000) * 0.20
        if (income > 9_00_000) tax += minOf(3_00_000.0, income - 9_00_000) * 0.15
        if (income > 6_00_000) tax += minOf(3_00_000.0, income - 6_00_000) * 0.10
        if (income > 3_00_000) tax += minOf(3_00_000.0, income - 3_00_000) * 0.05
        return tax
    }

    private fun applyNewRebateFy2425(
        income: Double,
        tax: Double,
    ): Double {
        if (income <= 7_00_000) return 0.0
        val excess = income - 7_00_000
        if (tax > excess) return excess
        return tax
    }

    private fun computeNewSlabTaxFy2526(income: Double): Double {
        var tax = 0.0
        if (income > 20_00_000) tax += (income - 20_00_000) * 0.30
        if (income > 16_00_000) tax += minOf(4_00_000.0, income - 16_00_000) * 0.20
        if (income > 12_00_000) tax += minOf(4_00_000.0, income - 12_00_000) * 0.15
        if (income > 8_00_000) tax += minOf(4_00_000.0, income - 8_00_000) * 0.10
        if (income > 4_00_000) tax += minOf(4_00_000.0, income - 4_00_000) * 0.05
        return tax
    }

    private fun applyNewRebateFy2526(
        income: Double,
        tax: Double,
    ): Double {
        if (income <= 12_00_000) return 0.0
        val excess = income - 12_00_000
        if (tax > excess) return excess
        return tax
    }

    private fun computeBreakEvenDeduction(
        gross: Double,
        targetTax: Double,
    ): Double {
        var low = 0.0
        var high = gross
        for (i in 0..20) {
            val mid = (low + high) / 2.0
            val oldTax = calculateOldRegimeTax(gross, mid).totalTaxPayable
            if (oldTax > targetTax) {
                low = mid
            } else {
                high = mid
            }
        }
        return high
    }
}
