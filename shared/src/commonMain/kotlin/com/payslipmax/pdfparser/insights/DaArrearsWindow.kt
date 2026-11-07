package com.payslipmax.pdfparser.insights

import kotlin.math.abs

/**
 * The months of DA arrears a payslip can be paying. A DA rise is always effective on 1 Jan or 1 Jul, but the order is
 * implemented later and the arrears land on whichever payslip follows, in the same half-year or a later one. So the months
 * owed are not fixed by the payslip's own half-year: each of the last [HALF_YEARS] effective dates is a candidate, and the
 * amount actually paid says which one it was. The single source for the Pay Audit arrears check and its pay-line explanation.
 */
internal object DaArrearsWindow {
    private const val HALF_YEARS = 3
    private const val MONTHS_PER_HALF_YEAR = 6
    private const val MONTHS_PER_YEAR = 12

    /**
     * Months of arrears if the rise took effect on each of the last [HALF_YEARS] 1 Jan / 1 Jul dates, fewest first. A count is
     * the months from the effective month up to the month before this payslip; a date with none yet owed is left out.
     * Empty for a [monthNum] outside 1..12 (a payslip whose month did not parse): no month, no window, nothing to audit.
     */
    fun candidateMonths(monthNum: Int): List<Int> {
        if (monthNum !in 1..MONTHS_PER_YEAR) return emptyList()
        val sinceHalfYearStart = (monthNum - 1) % MONTHS_PER_HALF_YEAR
        return (0 until HALF_YEARS).map { sinceHalfYearStart + it * MONTHS_PER_HALF_YEAR }.filter { it >= 1 }
    }

    /** The fewest months the payslip's arrears can cover; the most a shortfall may be measured against. Null only when [monthNum] is not a month 1..12. */
    fun minimumMonths(monthNum: Int): Int? = candidateMonths(monthNum).firstOrNull()

    /** The candidate whose [perMonth] x months is nearest the [actual] arrears paid; null only when [monthNum] is not a month 1..12. */
    fun closestMonths(
        monthNum: Int,
        actual: Double,
        perMonth: Double,
    ): Int? = candidateMonths(monthNum).minByOrNull { abs(actual - perMonth * it) }
}
