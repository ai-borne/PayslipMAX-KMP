package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.tax.PensionRuleKnowledgeBase

/**
 * The arithmetic of the year-end accumulation of annual leave (ODP-2026 pp.133-134), as pure numbers: no code, slot or payslip. The caps
 * are the manual's (note 5: 60 days, note 6: 30 days, (a): restricted to 300); 300 is the Leave Bank's own figure, so there is one copy of it.
 */
object AcclveCalculator {
    /** Annual leave a year may use together with what is accumulated for it (p.134 note 5), and the most that may be accumulated in a year (note 6). */
    const val YEAR_USE_CAP_DAYS = 60
    const val ACCRUAL_CAP_DAYS = 30

    /** What the previous total is restricted to (p.133 (a)); the Leave Bank's `LEAVE_MAX_DAYS`, never retyped. */
    val TOTAL_CAP_DAYS: Int = PensionRuleKnowledgeBase.LEAVE_MAX_DAYS

    /** (c): the days to accumulate for a year in which [alUsed] days of annual leave were used: the 30-day limit or what is left of 60. */
    fun accrual(alUsed: Int): Int = minOf(ACCRUAL_CAP_DAYS, (YEAR_USE_CAP_DAYS - alUsed).coerceAtLeast(0))

    /** (a) as used in the sum: the previous total, restricted to the cap. */
    fun capped(previous: Int): Int = previous.coerceAtLeast(0).coerceAtMost(TOTAL_CAP_DAYS)

    /** Whether the previous total is above the cap, so (a) carries "(Restricted to 300)". */
    fun restricted(previous: Int): Boolean = previous > TOTAL_CAP_DAYS

    /** (d) = (a) - (b) + (c), with (a) restricted. Not capped again: the statement itself shows 301 (Round 30). */
    fun total(
        previous: Int,
        ltcDays: Int,
        accrued: Int,
    ): Int = capped(previous) - ltcDays + accrued

    /** The first day the order may be published for [year]: 1 March of the next year (p.134 note 7); null when the calendar cannot hold it. */
    fun earliestPublication(year: Int): CalendarDate? =
        CalendarDate(year + 1, MARCH, 1).takeIf { CivilCalendar.exists(it) }

    /** Whether publishing on [today] is before the first allowed day for [year]. Unknown day or year judges nothing; retirement is the manual's exception. */
    fun publishedTooEarly(
        today: CalendarDate?,
        year: Int,
        retiring: Boolean,
    ): Boolean {
        if (today == null || retiring) return false
        val first = earliestPublication(year) ?: return false
        return CivilCalendar.dayNumber(today) < CivilCalendar.dayNumber(first)
    }

    private const val MARCH = 3
}
