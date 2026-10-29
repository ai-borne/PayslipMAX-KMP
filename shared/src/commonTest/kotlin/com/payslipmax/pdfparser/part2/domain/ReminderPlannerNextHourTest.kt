package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (Phase 11 item 35): a Due screen left open kept its old "coming up" and "overdue" labels, because nothing told it the clock had moved on.
 * A reminder falls due on the hour and "overdue" turns over at midnight, so the screen asks the planner how long to wait for the next local
 * hour. The hour arithmetic stays in the one place that knows the officer's zone (a half-hour zone's hours are not UTC's). Synthetic instants only.
 */
class ReminderPlannerNextHourTest : ReminderTestBase() {
    private val day = date(2026, 10, 9)

    @Test
    fun halfAnHourBeforeLocalMidnightTheWaitIsHalfAnHour() {
        assertEquals(30 * 60_000L, planner.millisUntilNextHour(at(day, hourOfDay = 23, minute = 30)))
    }

    @Test
    fun aMinuteBeforeNineTheWaitIsOneMinuteSoTheNineOClockReminderIsNotMissed() {
        assertEquals(60_000L, planner.millisUntilNextHour(at(day, hourOfDay = 8, minute = 59)))
    }

    @Test
    fun onTheHourExactlyTheWaitIsAWholeHourNeverZero() {
        assertEquals(hour, planner.millisUntilNextHour(at(day, hourOfDay = 9)))
    }

    @Test
    fun theOfficersZoneDecidesWhereTheHourFallsAHalfHourZoneIsNotUtc() {
        val instant = at(day, hourOfDay = 23, minute = 10, offset = 0L)
        assertEquals(50 * 60_000L, ReminderPlanner { 0L }.millisUntilNextHour(instant), "a clock on UTC: ten past, fifty minutes to go")
        assertEquals(20 * 60_000L, planner.millisUntilNextHour(instant), "the same instant is 04:40 in India: twenty minutes to 05:00")
    }
}
