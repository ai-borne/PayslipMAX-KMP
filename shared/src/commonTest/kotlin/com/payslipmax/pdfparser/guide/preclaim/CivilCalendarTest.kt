package com.payslipmax.pdfparser.guide.preclaim

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization of the civil-day count the pre-claim worksheet has always used, written before it moved to `CivilCalendar` and
 * unchanged after: the move must not change a single answer, because Total Absence multiplies every daily rate and Part II leave
 * orders (note 1 of the annual leave notes counts days inclusively) now depend on the same count. It goes through [DutyDays] and
 * [CalendarDate], the names the pre-claim code still uses; `DutyDaysTest` pins the owner's two claims, this pins the whole calendar.
 */
class CivilCalendarTest {
    private fun span(
        from: CalendarDate,
        to: CalendarDate,
    ) = DutyDays.inclusive(from, to)

    private fun lengthOf(
        year: Int,
        month: Int,
    ): Int = if (month == 2) (if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28) else listOf(31, 0, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[month - 1]

    @Test
    fun theWholeAllowedRangeCountsEveryDayOnce() {
        // 101 years of 365 days plus 25 leap days (2000, 2004 ... 2096; 2100 is not a leap year).
        assertEquals(DutySpan.Days(101 * 365 + 25), span(CalendarDate(2000, 1, 1), CalendarDate(2100, 12, 31)))
    }

    @Test
    fun everyWholeMonthCountsItsOwnLengthInclusively() {
        for (year in listOf(2023, 2024, 2000, 2100)) {
            for (month in 1..12) {
                val length = lengthOf(year, month)
                assertEquals(DutySpan.Days(length), span(CalendarDate(year, month, 1), CalendarDate(year, month, length)), "$year-$month")
            }
        }
    }

    @Test
    fun theLastDayOfEveryMonthExistsAndTheNextDayDoesNot() {
        for (year in listOf(2023, 2024, 2100)) {
            for (month in 1..12) {
                val length = lengthOf(year, month)
                val over = DutyDays.inclusive(CalendarDate(year, month, length + 1), CalendarDate(2100, 12, 31))
                assertEquals(DutySpan.Invalid(PreClaimIssue.DATE_INVALID), over, "$year-$month-${length + 1} does not exist")
                assertEquals(DutySpan.Days(1), span(CalendarDate(year, month, length), CalendarDate(year, month, length)), "$year-$month-$length exists")
            }
        }
    }

    @Test
    fun aYearCountsItsLengthWhicheverDayItStartsOn() {
        assertEquals(DutySpan.Days(365), span(CalendarDate(2023, 1, 1), CalendarDate(2023, 12, 31)))
        assertEquals(DutySpan.Days(366), span(CalendarDate(2024, 1, 1), CalendarDate(2024, 12, 31)))
        assertEquals(DutySpan.Days(365), span(CalendarDate(2023, 7, 5), CalendarDate(2024, 7, 3)), "a year less a day, both ends counted")
        assertEquals(DutySpan.Days(62), span(CalendarDate(2026, 7, 5), CalendarDate(2026, 9, 4)), "05 Jul to 04 Sep is 62 days, not 60")
    }

    @Test
    fun theCountIsTheSameWhicheverYearItFallsIn() {
        for (year in 2001..2099 step 7) {
            assertEquals(DutySpan.Days(28 + if (year % 4 == 0 && year % 100 != 0) 1 else 0), span(CalendarDate(year, 2, 1), CalendarDate(year, 2, 28 + if (year % 4 == 0 && year % 100 != 0) 1 else 0)), "$year")
            assertEquals(DutySpan.Days(2), span(CalendarDate(year, 12, 31), CalendarDate(year + 1, 1, 1)), "year end $year")
        }
    }

    @Test
    fun aReversedRangeIsRefusedWhateverTheDistance() {
        assertEquals(DutySpan.Invalid(PreClaimIssue.DATES_REVERSED), span(CalendarDate(2026, 1, 2), CalendarDate(2026, 1, 1)))
        assertEquals(DutySpan.Invalid(PreClaimIssue.DATES_REVERSED), span(CalendarDate(2100, 12, 31), CalendarDate(2000, 1, 1)))
    }

    @Test
    fun aDateIsCheckedBeforeTheRangeSoABadReversedPairIsInvalidNotReversed() {
        assertEquals(DutySpan.Invalid(PreClaimIssue.DATE_INVALID), span(CalendarDate(2026, 2, 30), CalendarDate(2026, 1, 1)))
    }
}
