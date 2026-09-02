package com.payslipmax.pdfparser.tax

/** Pure slab-tax math -- no rebate, no cess, no surcharge. Shared by both regimes (D2 SSOT). */
object SlabTaxCalculator {
    fun computeTax(
        netTaxableIncome: Double,
        slabs: List<TaxSlab>,
    ): Double {
        var tax = 0.0
        for (slab in slabs) {
            if (netTaxableIncome > slab.minIncome) {
                val upper = slab.maxIncome?.let { minOf(netTaxableIncome, it) } ?: netTaxableIncome
                tax += (upper - slab.minIncome) * slab.taxRatePct
            }
        }
        return tax
    }

    /** The rate that applies to the next rupee earned at [netTaxableIncome]. */
    fun marginalRate(
        netTaxableIncome: Double,
        slabs: List<TaxSlab>,
    ): Double {
        val activeSlab =
            slabs.lastOrNull { slab ->
                netTaxableIncome > slab.minIncome && (slab.maxIncome == null || netTaxableIncome <= slab.maxIncome)
            }
        return activeSlab?.taxRatePct ?: 0.0
    }
}
