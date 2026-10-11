package com.payslipmax.pdfparser.guide.preclaim

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Total Absence on the portal is the To date minus the From date, both days counted (plan 21: 57 and 61 days on the owner's two
 * claims). The count has to be exact across month ends, leap days and year ends, because it multiplies every daily rate.
 */
class DutyDaysTest {
    private fun date(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(year, month, day)

    private fun days(
        from: CalendarDate?,
        to: CalendarDate?,
    ) = DutyDays.inclusive(from, to)

    @Test
    fun theOwnersTwoClaimsCountFiftySevenAndSixtyOneDays() {
        assertEquals(DutySpan.Days(57), days(date(2024, 2, 1), date(2024, 3, 28)), "claim A spans the 2024 leap day")
        assertEquals(DutySpan.Days(61), days(date(2024, 4, 1), date(2024, 5, 31)), "claim B")
    }

    @Test
    fun aTripWithinOneDayIsOneDayAndTheEndsAreBothCounted() {
        assertEquals(DutySpan.Days(1), days(date(2026, 10, 11), date(2026, 10, 11)))
        assertEquals(DutySpan.Days(2), days(date(2026, 10, 11), date(2026, 10, 12)))
    }

    @Test
    fun monthEndsLeapDaysAndYearEndsAreExact() {
        assertEquals(DutySpan.Days(3), days(date(2024, 2, 28), date(2024, 3, 1)), "2024 has a 29 February")
        assertEquals(DutySpan.Days(2), days(date(2023, 2, 28), date(2023, 3, 1)), "2023 does not")
        assertEquals(DutySpan.Days(4), days(date(2024, 12, 30), date(2025, 1, 2)))
        assertEquals(DutySpan.Days(366), days(date(2024, 1, 1), date(2024, 12, 31)))
    }

    @Test
    fun aCenturyYearIsALeapYearOnlyWhenDivisibleBy400() {
        assertEquals(DutySpan.Days(2), days(date(2100, 2, 28), date(2100, 3, 1)), "2100 is not a leap year")
        assertEquals(DutySpan.Days(3), days(date(2000, 2, 28), date(2000, 3, 1)), "2000 is")
    }

    @Test
    fun aMissingDateOrAReversedRangeIsRefused() {
        assertEquals(DutySpan.Invalid(PreClaimIssue.DATES_MISSING), days(null, date(2026, 1, 1)))
        assertEquals(DutySpan.Invalid(PreClaimIssue.DATES_MISSING), days(date(2026, 1, 1), null))
        assertEquals(DutySpan.Invalid(PreClaimIssue.DATES_REVERSED), days(date(2026, 1, 2), date(2026, 1, 1)))
    }

    @Test
    fun aDateThatDoesNotExistIsRefusedNotRolledOver() {
        for (bad in listOf(date(2023, 2, 29), date(2026, 13, 1), date(2026, 0, 1), date(2026, 4, 31), date(2026, 1, 0), date(1999, 1, 1), date(2101, 1, 1))) {
            assertEquals(DutySpan.Invalid(PreClaimIssue.DATE_INVALID), days(bad, date(2026, 1, 1)), "$bad as From")
            assertEquals(DutySpan.Invalid(PreClaimIssue.DATE_INVALID), days(date(2000, 1, 1), bad), "$bad as To")
        }
    }
}
