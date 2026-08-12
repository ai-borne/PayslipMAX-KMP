package com.payslipmax.pdfparser.tax

/**
 * Surcharge on income tax for net taxable income above Rs 50L (D14/ADR-1). Old regime: 10/15/25/37%
 * at 50L/1Cr/2Cr/5Cr. New regime: same thresholds but capped at 25% (no 37% tier) since Budget 2023.
 * Includes marginal relief so total tax+surcharge never grows faster than income across a threshold.
 *
 * Not FY-versioned: these thresholds/rates are current law and, since no committed corpus fixture has
 * net taxable income above Rs 50L, this is unvalidated against real payslips -- correctness rests on
 * the public CBDT rate schedule, not corpus evidence (see docs/AI_INSIGHTS_PIPELINE.md SWOT conventions).
 */
object SurchargeCalculator {
    private val SHARED_TIERS = listOf(5_000_000.0 to 0.10, 10_000_000.0 to 0.15, 20_000_000.0 to 0.25)
    private val OLD_REGIME_ONLY_TOP_TIER = 50_000_000.0 to 0.37

    fun computeSurcharge(
        netTaxableIncome: Double,
        baseTax: Double,
        slabs: List<TaxSlab>,
        isNewRegime: Boolean,
    ): Double {
        val tiers = if (isNewRegime) SHARED_TIERS else SHARED_TIERS + OLD_REGIME_ONLY_TOP_TIER
        val tierIndex = tiers.indexOfLast { (threshold, _) -> netTaxableIncome > threshold }
        if (tierIndex < 0) return 0.0

        val (threshold, rate) = tiers[tierIndex]
        val previousRate = if (tierIndex == 0) 0.0 else tiers[tierIndex - 1].second
        val rawSurcharge = baseTax * rate

        val taxAtThreshold = SlabTaxCalculator.computeTax(threshold, slabs)
        val totalAtThreshold = taxAtThreshold * (1 + previousRate)
        val maxAllowedTotal = totalAtThreshold + (netTaxableIncome - threshold)
        val totalAtIncome = baseTax + rawSurcharge

        return if (totalAtIncome > maxAllowedTotal) {
            maxOf(0.0, maxAllowedTotal - baseTax)
        } else {
            rawSurcharge
        }
    }
}
