package com.payslipmax.pdfparser.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The calendar both the TA/DA pre-claim and Part II orders count with. Its answers are pinned by `CivilCalendarTest` (through the
 * pre-claim names, unchanged by the move); this pins the API the Part II engine calls directly, so a slip's day count, its date
 * order and its printed dates cannot drift from the worksheet's.
 */
class CivilCalendarApiTest {
    private fun date(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(year, month, day)

    @Test
    fun aDateExistsOnlyInsideTheAllowedYearsAndRealMonths() {
        assertTrue(CivilCalendar.exists(date(2024, 2, 29)))
        assertTrue(CivilCalendar.exists(date(2000, 1, 1)) && CivilCalendar.exists(date(2100, 12, 31)))
        for (bad in listOf(date(2023, 2, 29), date(2026, 4, 31), date(2026, 13, 1), date(2026, 0, 1), date(2026, 1, 0), date(1999, 12, 31), date(2101, 1, 1))) {
            assertFalse(CivilCalendar.exists(bad), "$bad")
        }
    }

    @Test
    fun daysBetweenIsTheSignedGapAndInclusiveCountsBothEnds() {
        assertEquals(0L, CivilCalendar.daysBetween(date(2026, 1, 1), date(2026, 1, 1)))
        assertEquals(61L, CivilCalendar.daysBetween(date(2026, 7, 5), date(2026, 9, 4)))
        assertEquals(-1L, CivilCalendar.daysBetween(date(2026, 1, 2), date(2026, 1, 1)))
        assertEquals(62, CivilCalendar.inclusiveDays(date(2026, 7, 5), date(2026, 9, 4)), "the leave notes' own example: 62, not 60")
        assertEquals(null, CivilCalendar.inclusiveDays(date(2026, 9, 4), date(2026, 7, 5)), "a reversed range has no count")
    }

    @Test
    fun theDayNumberOrdersDatesAcrossMonthsAndYears() {
        val dates = listOf(date(2026, 3, 1), date(2025, 12, 31), date(2026, 2, 28), date(2024, 2, 29), date(2026, 3, 2))
        val sorted = dates.sortedBy { CivilCalendar.dayNumber(it) }
        assertEquals(listOf(date(2024, 2, 29), date(2025, 12, 31), date(2026, 2, 28), date(2026, 3, 1), date(2026, 3, 2)), sorted)
        assertEquals(1L, CivilCalendar.dayNumber(date(2026, 3, 2)) - CivilCalendar.dayNumber(date(2026, 3, 1)))
    }

    @Test
    fun aDateIsPrintedDayMonthYearWithLeadingZeros() {
        assertEquals("05/07/2026", CivilCalendar.format(date(2026, 7, 5)))
        assertEquals("31/12/2099", CivilCalendar.format(date(2099, 12, 31)))
        assertEquals("01/01/2000", CivilCalendar.format(date(2000, 1, 1)))
    }
}
