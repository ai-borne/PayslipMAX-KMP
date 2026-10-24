package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The calendar picker speaks UTC milliseconds at midnight. The conversion has no time zone and no platform date library, so a wrong
 * answer would shift every claim by a day: it must be exact on known days, on leap days, across the whole range the picker offers,
 * and for a time that is not exactly midnight.
 */
class PreClaimDatesTest {
    private val day = 86_400_000L

    @Test
    fun knownDaysConvertBothWays() {
        assertEquals(0L, PreClaimDates.toUtcMillis(CalendarDate(1970, 1, 1)))
        assertEquals(1_706_745_600_000L, PreClaimDates.toUtcMillis(CalendarDate(2024, 2, 1)), "the first day of claim A")
        assertEquals(CalendarDate(2024, 2, 1), PreClaimDates.fromUtcMillis(1_706_745_600_000L))
        assertEquals(CalendarDate(2024, 3, 28), PreClaimDates.fromUtcMillis(PreClaimDates.toUtcMillis(CalendarDate(2024, 3, 28))))
    }

    @Test
    fun leapDaysAndYearEndsAreExact() {
        assertEquals(CalendarDate(2024, 2, 29), PreClaimDates.fromUtcMillis(PreClaimDates.toUtcMillis(CalendarDate(2024, 2, 29))))
        assertEquals(CalendarDate(2000, 2, 29), PreClaimDates.fromUtcMillis(PreClaimDates.toUtcMillis(CalendarDate(2000, 2, 29))), "2000 is a leap year (divisible by 400)")
        assertEquals(CalendarDate(2100, 3, 1), PreClaimDates.fromUtcMillis(PreClaimDates.toUtcMillis(CalendarDate(2100, 2, 28)) + day), "2100 is not a leap year")
        assertEquals(CalendarDate(2025, 1, 1), PreClaimDates.fromUtcMillis(PreClaimDates.toUtcMillis(CalendarDate(2024, 12, 31)) + day))
    }

    @Test
    fun everyDayOfTheRangeThePickerOffersRoundTripsAndFollowsItsNeighbour() {
        var date = CalendarDate(PreClaimDates.YEARS.first, 1, 1)
        var previous = PreClaimDates.toUtcMillis(date) - day
        val last = CalendarDate(PreClaimDates.YEARS.last, 12, 31)
        while (date != last) {
            val millis = PreClaimDates.toUtcMillis(date)
            assertEquals(previous + day, millis, "$date is not one day after the day before")
            assertEquals(date, PreClaimDates.fromUtcMillis(millis))
            previous = millis
            date = next(date)
        }
    }

    @Test
    fun aTimeInsideTheDayIsStillThatDay() {
        val noon = PreClaimDates.toUtcMillis(CalendarDate(2026, 3, 5)) + day / 2
        assertEquals(CalendarDate(2026, 3, 5), PreClaimDates.fromUtcMillis(noon))
        assertEquals(CalendarDate(2026, 3, 5), PreClaimDates.fromUtcMillis(noon + day / 2 - 1))
    }

    @Test
    fun anInstantFallsOnTheDayWhereTheDeviceIs() {
        val lateUtc = 1_792_785_600_000L // 2026-10-23 20:00 UTC
        assertEquals(CalendarDate(2026, 10, 24), PreClaimDates.dayOfInstant(lateUtc, 19_800_000L), "India is already on the 24th")
        assertEquals(CalendarDate(2026, 10, 23), PreClaimDates.dayOfInstant(lateUtc, 0L))
        assertEquals(CalendarDate(2026, 10, 23), PreClaimDates.dayOfInstant(lateUtc, -14_400_000L))
        assertEquals(CalendarDate(2026, 10, 23), PreClaimDates.dayOfInstant(1_792_807_200_000L, -18_000_000L - 3_600_000L * 4), "02:00 UTC on the 24th is the evening of the 23rd at -9 hours")
    }

    @Test
    fun theLocalMidnightIsTheFirstInstantOfTheNewDay() {
        val midnightIst = PreClaimDates.toUtcMillis(CalendarDate(2026, 10, 24)) - 19_800_000L
        assertEquals(CalendarDate(2026, 10, 24), PreClaimDates.dayOfInstant(midnightIst, 19_800_000L))
        assertEquals(CalendarDate(2026, 10, 23), PreClaimDates.dayOfInstant(midnightIst - 1, 19_800_000L))
    }

    @Test
    fun aDateIsWrittenWithTwoDigitDayAndMonth() {
        assertEquals("05/03/2026", PreClaimDates.format(CalendarDate(2026, 3, 5)))
        assertEquals("31/12/2100", PreClaimDates.format(CalendarDate(2100, 12, 31)))
        assertEquals("", PreClaimDates.format(null))
    }

    private fun next(date: CalendarDate): CalendarDate {
        val lengths = intArrayOf(31, if (isLeap(date.year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return when {
            date.day < lengths[date.month - 1] -> date.copy(day = date.day + 1)
            date.month < 12 -> date.copy(month = date.month + 1, day = 1)
            else -> CalendarDate(date.year + 1, 1, 1)
        }
    }

    private fun isLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
