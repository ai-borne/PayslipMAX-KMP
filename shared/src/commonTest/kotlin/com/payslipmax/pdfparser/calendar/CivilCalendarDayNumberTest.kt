package com.payslipmax.pdfparser.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The inverse of `dayNumber` and the Unix offset the reminder planner turns days into instants with. Known answers come from outside this
 * file (the Unix times of two midnights), so a wrong constant or a wrong leap rule cannot pass by agreeing with itself.
 */
class CivilCalendarDayNumberTest {
    private val millisPerDay = 86_400_000L

    @Test
    fun theUnixOffsetMatchesTwoKnownMidnights() {
        // why: 946684800 s is 2000-01-01T00:00Z and 1767225600 s is 2026-01-01T00:00Z; the planner's instants stand on this constant.
        assertEquals(946_684_800_000L, CivilCalendar.UNIX_DAYS_TO_2000 * millisPerDay)
        val day2026 = CivilCalendar.dayNumber(CalendarDate(2026, 1, 1))
        assertEquals(1_767_225_600_000L, (day2026 + CivilCalendar.UNIX_DAYS_TO_2000) * millisPerDay)
    }

    @Test
    fun everyDayOfTheAllowedYearsRoundTrips() {
        var day = 0L
        var date: CalendarDate? = CalendarDate(CivilCalendar.FIRST_YEAR, 1, 1)
        while (date != null) {
            assertEquals(day, CivilCalendar.dayNumber(date), "day number of $date")
            assertEquals(date, CivilCalendar.fromDayNumber(day), "date of day $day")
            date = CivilCalendar.nextDay(date)
            day++
        }
        assertEquals(CalendarDate(CivilCalendar.LAST_YEAR, 12, 31), CivilCalendar.fromDayNumber(day - 1))
    }

    @Test
    fun leapDaysAreFoundWhereTheyBelong() {
        assertEquals(CalendarDate(2028, 2, 29), CivilCalendar.fromDayNumber(CivilCalendar.dayNumber(CalendarDate(2028, 2, 28)) + 1))
        assertEquals(CalendarDate(2100, 3, 1), CivilCalendar.fromDayNumber(CivilCalendar.dayNumber(CalendarDate(2100, 2, 28)) + 1))
    }

    @Test
    fun aDayOutsideTheAllowedYearsIsNull() {
        assertNull(CivilCalendar.fromDayNumber(-1))
        assertNull(CivilCalendar.fromDayNumber(CivilCalendar.dayNumber(CalendarDate(CivilCalendar.LAST_YEAR, 12, 31)) + 1))
    }
}
