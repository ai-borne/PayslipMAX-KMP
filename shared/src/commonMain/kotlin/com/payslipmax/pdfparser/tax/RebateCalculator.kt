package com.payslipmax.pdfparser.tax

/**
 * Section 87A rebate, both regimes. Old regime is a hard cliff (no marginal relief provision in law);
 * new regime has statutory marginal relief just above its threshold.
 */
object RebateCalculator {
    fun applyOldRegimeRebate(
        netTaxableIncome: Double,
        rawTax: Double,
        maxRebateIncome: Double,
        rebateCap: Double,
    ): Double {
        if (netTaxableIncome <= maxRebateIncome) {
            return maxOf(0.0, rawTax - rebateCap)
        }
        return rawTax
    }

    fun applyNewRegimeRebate(
        netTaxableIncome: Double,
        rawTax: Double,
        maxRebateIncome: Double,
        rebateCap: Double,
    ): Double {
        if (netTaxableIncome <= maxRebateIncome) {
            return maxOf(0.0, rawTax - minOf(rawTax, rebateCap))
        }
        val incomeAboveThreshold = netTaxableIncome - maxRebateIncome
        return if (rawTax > incomeAboveThreshold) incomeAboveThreshold else rawTax
    }
}
