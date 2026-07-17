package com.payslipmax.pdfparser.parser.detection

/**
 * A payslip's printed statement period (the month/year the document covers), used as the
 * primary, structural signal for grammar-era selection in [com.payslipmax.pdfparser.parser.registry.GrammarRegistry].
 */
data class StatementPeriod(
    val month: Int,
    val year: Int,
) {
    /** Monotonic month index (Jan year 0 = 0), used for range comparisons in [GrammarEraMapper]. */
    val ordinal: Int get() = year * 12 + month
}
