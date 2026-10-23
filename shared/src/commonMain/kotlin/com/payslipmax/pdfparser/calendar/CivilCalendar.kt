package com.payslipmax.pdfparser.calendar

/** A calendar date the officer picked. No time zone and no clock: it is only counted against another date. */
data class CalendarDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

/**
 * Plain civil-calendar arithmetic with no platform date library, so Android and iOS count alike. It is the one calendar the TA/DA
 * pre-claim (Total Absence, both days counted) and the Part II orders (leave days, date order, printed dates) share. Dates are limited
 * to 2000-2100 because a pay-matrix claim or a leave order outside that is a typing slip, and a date that does not exist (31 April) is
 * refused, not rolled over. Moved here from the pre-claim package with every answer unchanged (`CivilCalendarTest` pins them).
 */
object CivilCalendar {
    const val FIRST_YEAR = 2000
    const val LAST_YEAR = 2100
    private const val MONTHS = 12
    private const val PART_DIGITS = 2
    private const val YEAR_DIGITS = 4
    private val monthLengths = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    fun exists(date: CalendarDate): Boolean {
        if (date.year !in FIRST_YEAR..LAST_YEAR || date.month !in 1..MONTHS) return false
        return date.day in 1..lengthOfMonth(date.year, date.month)
    }

    /** Days since 2000-01-01 (the first allowed year), so only differences and order matter. The date must [exist]. */
    fun dayNumber(date: CalendarDate): Long {
        var days = 0L
        for (year in FIRST_YEAR until date.year) days += if (isLeap(year)) 366 else 365
        for (month in 1 until date.month) days += lengthOfMonth(date.year, month)
        return days + date.day - 1
    }

    /** [to] minus [from] in days: negative when [to] is earlier. Both dates must [exist]. */
    fun daysBetween(
        from: CalendarDate,
        to: CalendarDate,
    ): Long = dayNumber(to) - dayNumber(from)

    /** Days from [from] to [to] with both days counted (05 Jul to 04 Sep is 62), or null when [to] is earlier. Both dates must [exist]. */
    fun inclusiveDays(
        from: CalendarDate,
        to: CalendarDate,
    ): Int? = daysBetween(from, to).takeIf { it >= 0 }?.let { it.toInt() + 1 }

    /** DD/MM/YYYY, the way HRMS prints a date. */
    fun format(date: CalendarDate): String =
        "${date.day.toString().padStart(PART_DIGITS, '0')}/${date.month.toString().padStart(PART_DIGITS, '0')}/${date.year.toString().padStart(YEAR_DIGITS, '0')}"

    private fun isLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    private fun lengthOfMonth(
        year: Int,
        month: Int,
    ) = monthLengths[month - 1] + if (month == 2 && isLeap(year)) 1 else 0
}
