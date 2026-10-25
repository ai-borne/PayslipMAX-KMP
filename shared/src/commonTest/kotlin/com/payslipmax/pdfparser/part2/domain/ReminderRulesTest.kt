package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: storage bounds the size and count of what a draft holds but not a number's value, so a restored backup can carry any leave year. The
 * calendar counts years by a loop, so the rule must refuse a year it cannot hold before anything is counted, or a crafted file would hang the
 * Due list. These pin the edges of that range on the rule itself, because the planner's answer is "no reminder" either way.
 */
class ReminderRulesTest : ReminderTestBase() {
    private fun annual(year: Int) = request("GTDAL", year(year))

    @Test
    fun aYearInsideTheCalendarIsKeptAtBothEdges() {
        assertEquals(2000, ReminderRules.annualYear(annual(2000)))
        assertEquals(2026, ReminderRules.annualYear(annual(2026)))
        assertEquals(2100, ReminderRules.annualYear(annual(2100)))
    }

    @Test
    fun aYearOutsideTheCalendarIsRefusedBeforeAnyCounting() {
        for (year in listOf(1999, 2101, 100_000, Int.MAX_VALUE, Int.MIN_VALUE, -1, 0)) {
            assertNull(ReminderRules.annualYear(annual(year)), "$year")
        }
    }

    @Test
    fun anAnnualYearComesOnlyFromAnAnnualGrantsNumberSlot() {
        assertNull(ReminderRules.annualYear(request("GTDCL", year(2026))))
        assertNull(ReminderRules.annualYear(request("GTDAL", "year" to SlotValue.TextValue("2026"))))
        assertNull(ReminderRules.annualYear(request("GTDAL")))
    }

    @Test
    fun aHugeLeaveYearInADraftGivesNoReminderAndNoHang() {
        val huge = draft("d1", annual(Int.MAX_VALUE), printedAt = at(date(2026, 9, 20)))
        assertEquals(emptyList(), plan(listOf(huge), now = at(date(2026, 10, 2))).of(ReminderKind.YEAR_END))
    }
}
