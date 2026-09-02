package com.payslipmax.pdfparser.tax.rules

import com.payslipmax.pdfparser.tax.TaxSlab

/**
 * New regime (Section 115BAC) slabs, FY 2023-24 -> FY 2026-27. Each entry is the *annual law* for that
 * FY -- not necessarily what PCDA's payroll withheld mid-year (see PCDA_LAG in PcdaTaxParityTest: PCDA
 * applied the Finance (No.2) Act 2024 slabs from the December 2024 payroll run onward, but the December
 * revision governs the whole of FY 2024-25 for filing purposes).
 *
 * Pre-FY2023-24 new-regime elections are out of scope (Section 115BAC did not exist before FY 2020-21,
 * the corpus never shows a "New Tax Regime" election before FY 2023-24, and every payslip defaulted to
 * OLD regime before then).
 */
object NewRegimeSlabTable {
    private val FY_2023_24 =
        listOf(
            TaxSlab(0.0, 300000.0, 0.0),
            TaxSlab(300000.0, 600000.0, 0.05),
            TaxSlab(600000.0, 900000.0, 0.10),
            TaxSlab(900000.0, 1200000.0, 0.15),
            TaxSlab(1200000.0, 1500000.0, 0.20),
            TaxSlab(1500000.0, null, 0.30),
        )

    private val FY_2024_25 =
        listOf(
            TaxSlab(0.0, 300000.0, 0.0),
            TaxSlab(300000.0, 700000.0, 0.05),
            TaxSlab(700000.0, 1000000.0, 0.10),
            TaxSlab(1000000.0, 1200000.0, 0.15),
            TaxSlab(1200000.0, 1500000.0, 0.20),
            TaxSlab(1500000.0, null, 0.30),
        )

    private val FY_2025_26_ONWARD =
        listOf(
            TaxSlab(0.0, 400000.0, 0.0),
            TaxSlab(400000.0, 800000.0, 0.05),
            TaxSlab(800000.0, 1200000.0, 0.10),
            TaxSlab(1200000.0, 1600000.0, 0.15),
            TaxSlab(1600000.0, 2000000.0, 0.20),
            TaxSlab(2000000.0, 2400000.0, 0.25),
            TaxSlab(2400000.0, null, 0.30),
        )

    /** [fy] in "YYYY-YY" form, e.g. "2024-25". */
    fun forFy(fy: String): List<TaxSlab> {
        val startYear = fy.substringBefore("-").toIntOrNull() ?: return FY_2025_26_ONWARD
        return when {
            startYear <= 2023 -> FY_2023_24
            startYear == 2024 -> FY_2024_25
            else -> FY_2025_26_ONWARD
        }
    }
}
