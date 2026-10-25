package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate

/**
 * Turns the calendar picker's answer into a [CalendarDate] and back, and writes a date as DD/MM/YYYY. The picker speaks UTC milliseconds
 * at midnight, so the conversion is plain day arithmetic on the civil calendar (Howard Hinnant's algorithms): no time zone, no clock and
 * no platform date library, and the same answer on Android and iOS.
 */
internal object PreClaimDates {
    /** The years the picker offers; the calculators accept the same range ([CivilCalendar]). */
    val YEARS = CivilCalendar.FIRST_YEAR..CivilCalendar.LAST_YEAR

    private const val MILLIS_PER_DAY = 86_400_000L
    private const val DAYS_TO_1970 = 719_468L
    private const val DAYS_PER_ERA = 146_097L
    private const val YEARS_PER_ERA = 400L
    private const val MARCH_SHIFT = 9
    private const val MONTHS_TO_MARCH = 3
    private const val JANUARY_FEBRUARY = 2
    private const val MONTH_STEP = 153
    private const val MONTH_BIAS = 2
    private const val MONTH_SPAN = 5
    private const val DAYS_PER_YEAR = 365
    private const val DAYS_PER_4_YEARS = 4
    private const val DAYS_PER_100_YEARS = 100
    private const val ERA_CYCLE_4 = 1460L
    private const val ERA_CYCLE_100 = 36_524L
    private const val ERA_CYCLE_400 = 146_096L
    private const val MARCH_FIRST_LAST_MONTH = 10

    fun toUtcMillis(date: CalendarDate): Long = daysFromCivil(date) * MILLIS_PER_DAY

    fun fromUtcMillis(millis: Long): CalendarDate = civilFromDays(millis.floorDiv(MILLIS_PER_DAY))

    /** The day an instant ([millis], epoch milliseconds) fell on where the device is: its UTC day moved by [offsetMillis] (east positive). */
    fun dayOfInstant(
        millis: Long,
        offsetMillis: Long,
    ): CalendarDate = fromUtcMillis(millis + offsetMillis)

    /** The day an instant fell on where the device is, [localOffset] giving the device's offset at that instant (it moves with daylight saving). */
    fun localDay(
        millis: Long,
        localOffset: (atMillis: Long) -> Long,
    ): CalendarDate = dayOfInstant(millis, localOffset(millis))

    fun format(date: CalendarDate?): String = date?.let(CivilCalendar::format).orEmpty()

    private fun daysFromCivil(date: CalendarDate): Long {
        val year = (if (date.month <= JANUARY_FEBRUARY) date.year - 1 else date.year).toLong()
        val era = year.floorDiv(YEARS_PER_ERA)
        val yearOfEra = year - era * YEARS_PER_ERA
        val monthFromMarch = (if (date.month > JANUARY_FEBRUARY) date.month - MONTHS_TO_MARCH else date.month + MARCH_SHIFT).toLong()
        val dayOfYear = (MONTH_STEP * monthFromMarch + MONTH_BIAS) / MONTH_SPAN + date.day - 1
        val dayOfEra = yearOfEra * DAYS_PER_YEAR + yearOfEra / DAYS_PER_4_YEARS - yearOfEra / DAYS_PER_100_YEARS + dayOfYear
        return era * DAYS_PER_ERA + dayOfEra - DAYS_TO_1970
    }

    private fun civilFromDays(days: Long): CalendarDate {
        val shifted = days + DAYS_TO_1970
        val era = shifted.floorDiv(DAYS_PER_ERA)
        val dayOfEra = shifted - era * DAYS_PER_ERA
        val yearOfEra = (dayOfEra - dayOfEra / ERA_CYCLE_4 + dayOfEra / ERA_CYCLE_100 - dayOfEra / ERA_CYCLE_400) / DAYS_PER_YEAR
        val dayOfYear = dayOfEra - (DAYS_PER_YEAR * yearOfEra + yearOfEra / DAYS_PER_4_YEARS - yearOfEra / DAYS_PER_100_YEARS)
        val monthFromMarch = (MONTH_SPAN * dayOfYear + MONTH_BIAS) / MONTH_STEP
        val day = dayOfYear - (MONTH_STEP * monthFromMarch + MONTH_BIAS) / MONTH_SPAN + 1
        val month = if (monthFromMarch < MARCH_FIRST_LAST_MONTH) monthFromMarch + MONTHS_TO_MARCH else monthFromMarch - MARCH_SHIFT
        val year = yearOfEra + era * YEARS_PER_ERA + if (month <= JANUARY_FEBRUARY) 1 else 0
        return CalendarDate(year.toInt(), month.toInt(), day.toInt())
    }
}
