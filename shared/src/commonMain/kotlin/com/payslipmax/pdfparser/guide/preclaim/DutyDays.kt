package com.payslipmax.pdfparser.guide.preclaim

/** A calendar date the officer picked. No time zone and no clock: it is only counted against another date. */
data class CalendarDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

internal sealed interface DutySpan {
    data class Days(val count: Int) : DutySpan

    data class Invalid(val issue: PreClaimIssue) : DutySpan
}

/**
 * Total Absence as the portal counts it: the To date minus the From date, both days counted (57 and 61 on the owner's claims).
 * Plain civil-calendar arithmetic, no platform date library, so Android and iOS count alike. Dates are limited to 2000-2100
 * because a pay-matrix claim outside that is a typing slip, and a date that does not exist (31 April) is refused, not rolled over.
 */
internal object DutyDays {
    private const val FIRST_YEAR = 2000
    private const val LAST_YEAR = 2100
    private const val MONTHS = 12
    private val monthLengths = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    fun inclusive(
        from: CalendarDate?,
        to: CalendarDate?,
    ): DutySpan {
        if (from == null || to == null) return DutySpan.Invalid(PreClaimIssue.DATES_MISSING)
        if (!exists(from) || !exists(to)) return DutySpan.Invalid(PreClaimIssue.DATE_INVALID)
        val span = dayNumber(to) - dayNumber(from)
        return if (span < 0) DutySpan.Invalid(PreClaimIssue.DATES_REVERSED) else DutySpan.Days(span.toInt() + 1)
    }

    private fun isLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    internal fun exists(date: CalendarDate): Boolean {
        if (date.year !in FIRST_YEAR..LAST_YEAR || date.month !in 1..MONTHS) return false
        val length = monthLengths[date.month - 1] + if (date.month == 2 && isLeap(date.year)) 1 else 0
        return date.day in 1..length
    }

    /** Days since 2000-01-01 (the first allowed year), so only differences matter. */
    internal fun dayNumber(date: CalendarDate): Long {
        var days = 0L
        for (year in FIRST_YEAR until date.year) days += if (isLeap(year)) 366 else 365
        for (month in 1 until date.month) days += monthLengths[month - 1] + if (month == 2 && isLeap(date.year)) 1 else 0
        return days + date.day - 1
    }
}
