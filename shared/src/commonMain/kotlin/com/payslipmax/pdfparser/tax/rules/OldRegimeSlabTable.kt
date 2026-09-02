package com.payslipmax.pdfparser.tax.rules

import com.payslipmax.pdfparser.tax.TaxSlab

/**
 * Old (default pre-115BAC) regime slabs, FY 2015-16 -> FY 2026-27. Unchanged since FY 2017-18; the
 * 2.5L-5L band was 10% before that (evidence: a constant +Rs 12,500 delta on every FY <= 2016-17 corpus
 * fixture -- see docs/Plan/04_TaxPlannerGoldStandard.md S1.3).
 */
object OldRegimeSlabTable {
    private val PRE_2017_18 =
        listOf(
            TaxSlab(0.0, 250000.0, 0.0),
            TaxSlab(250000.0, 500000.0, 0.10),
            TaxSlab(500000.0, 1000000.0, 0.20),
            TaxSlab(1000000.0, null, 0.30),
        )

    private val FROM_2017_18 =
        listOf(
            TaxSlab(0.0, 250000.0, 0.0),
            TaxSlab(250000.0, 500000.0, 0.05),
            TaxSlab(500000.0, 1000000.0, 0.20),
            TaxSlab(1000000.0, null, 0.30),
        )

    /** [fy] in "YYYY-YY" form, e.g. "2024-25". */
    fun forFy(fy: String): List<TaxSlab> {
        val startYear = fy.substringBefore("-").toIntOrNull() ?: return FROM_2017_18
        return if (startYear <= 2016) PRE_2017_18 else FROM_2017_18
    }
}
