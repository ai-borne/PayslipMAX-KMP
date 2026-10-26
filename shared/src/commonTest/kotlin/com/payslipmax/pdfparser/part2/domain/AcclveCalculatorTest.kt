package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The year-end accumulation of annual leave as numbers (ODP-2026 pp.133-134, notes 5, 6 and 7), including the worked example the manual
 * prints (a restricted 380 less 10 plus 0 is 290) and the figure a real statement shows (282 plus 19 is 301). Each test says what an
 * audit would reject.
 */
class AcclveCalculatorTest {
    @Test
    fun theDaysToAccumulateAreThirtyOrWhatIsLeftOfSixtyAfterTheYearsAnnualLeave() {
        // why: note 6 - at most 30 a year; note 5 - what is accumulated plus the leave used may not pass 60.
        assertEquals(30, AcclveCalculator.accrual(0))
        assertEquals(30, AcclveCalculator.accrual(30), "60 - 30 is exactly 30")
        assertEquals(29, AcclveCalculator.accrual(31))
        assertEquals(1, AcclveCalculator.accrual(59))
        assertEquals(0, AcclveCalculator.accrual(60))
        assertEquals(0, AcclveCalculator.accrual(75), "more than 60 used leaves nothing, never a negative figure")
        assertEquals(30, AcclveCalculator.accrual(-5), "a negative count of days used is not more leave")
    }

    @Test
    fun thePreviousTotalIsRestrictedToThreeHundredAndSaysSoOnlyAboveIt() {
        assertEquals(282, AcclveCalculator.capped(282))
        assertEquals(300, AcclveCalculator.capped(300))
        assertEquals(300, AcclveCalculator.capped(301))
        assertEquals(300, AcclveCalculator.capped(380))
        assertEquals(0, AcclveCalculator.capped(-1))
        assertFalse(AcclveCalculator.restricted(300))
        assertTrue(AcclveCalculator.restricted(301))
        assertEquals(300, AcclveCalculator.TOTAL_CAP_DAYS, "the Leave Bank's figure")
    }

    @Test
    fun theTotalIsAMinusBPlusCWithAThatIsRestrictedAndTheResultNotCappedAgain() {
        assertEquals(290, AcclveCalculator.total(380, 10, 0), "the manual's own example: (300-10+00)")
        assertEquals(290, AcclveCalculator.total(300, 10, 0))
        assertEquals(301, AcclveCalculator.total(282, 0, 19), "a real statement shows 301, so the total is not cut to 300")
        assertEquals(0, AcclveCalculator.total(0, 0, 0))
        assertEquals(19, AcclveCalculator.total(0, 0, 19))
    }

    @Test
    fun theOrderMayBePublishedFromTheFirstOfMarchOfTheNextYear() {
        // why: note 7 - on or after 1 March of the following year, or on the date of retirement.
        assertEquals(CalendarDate(2026, 3, 1), AcclveCalculator.earliestPublication(2025))
        assertNull(AcclveCalculator.earliestPublication(2100), "2101 is outside the calendar")

        fun early(
            today: CalendarDate?,
            retiring: Boolean = false,
        ) = AcclveCalculator.publishedTooEarly(today, 2025, retiring)
        assertTrue(early(CalendarDate(2026, 2, 28)))
        assertFalse(early(CalendarDate(2026, 3, 1)), "1 March itself is allowed")
        assertFalse(early(CalendarDate(2026, 10, 1)))
        assertTrue(early(CalendarDate(2025, 12, 31)), "the year itself is not over")
        assertFalse(early(CalendarDate(2026, 2, 28), retiring = true), "on retirement it may be published any day")
        assertFalse(early(null), "no device day judges nothing")
        assertFalse(AcclveCalculator.publishedTooEarly(CalendarDate(2026, 2, 28), 2100, false), "a year the calendar cannot hold judges nothing")
    }
}
