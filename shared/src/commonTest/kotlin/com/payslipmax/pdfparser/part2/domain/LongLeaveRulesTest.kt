package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import com.payslipmax.pdfparser.part2.domain.RuleContexts.date
import com.payslipmax.pdfparser.part2.domain.RuleContexts.neighbour
import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import com.payslipmax.pdfparser.part2.domain.RuleContexts.ref
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The computable notes of the rest of Chapter 5 (Part II plan, phase 8) as the checks the officer sees. Each test says why the note
 * matters: an order that breaks one is queried or rejected in audit. Dates are synthetic.
 */
class LongLeaveRulesTest {
    private fun check(id: String) = assertNotNull(LongLeaveRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == id }, "rule $id")

    private fun days(count: Int) = mapOf("days" to number(count))

    @Test
    fun theFamilyOffersExactlyTheChecksTheChapterFiveTemplatesCite() {
        assertEquals(
            setOf(
                "furlough_cycle", "furlough_not_ncc", "ccl_limits", "terminal_leave_28", "terminal_with_annual", "adoption_leave_180",
                "paternity_adoption_15", "grant_quoted_furlough", "grant_quoted_study", "grant_quoted_overstay", "declared_quoted_overstay",
                "declared_quoted_awl", "reissue_allowances",
            ),
            LongLeaveRules.all.map { it.id }.toSet(),
        )
        assertTrue(LongLeaveRules.all.all { it is CheckRule }, "this family holds checks only")
    }

    @Test
    fun aFurloughCycleIsThreeYearsAndTheLeaveFallsInsideIt() {
        // why: p.118 note 4 - furlough is granted for a three-year cycle, during that cycle only, never for a previous one.
        val rule = check(LongLeaveRules.FURLOUGH_CYCLE)

        fun cycle(
            from: Int,
            to: Int,
            start: Int = 2026,
        ) = mapOf("cycle_from" to number(from), "cycle_to" to number(to), "leave_from" to date(7, 6, start))
        assertFalse(rule.violated(context("X", cycle(2025, 2027))), "a three-year block holding the leave")
        assertFalse(rule.violated(context("X", cycle(2026, 2028))), "the leave in the first year")
        assertFalse(rule.violated(context("X", cycle(2024, 2026))), "the leave in the last year")
        assertTrue(rule.violated(context("X", cycle(2025, 2026))), "two years is not a three-year cycle")
        assertTrue(rule.violated(context("X", cycle(2025, 2028))), "four years is not a three-year cycle")
        assertTrue(rule.violated(context("X", cycle(2021, 2023))), "a cycle already over")
        assertTrue(rule.violated(context("X", cycle(2027, 2029))), "a cycle not begun")
        assertFalse(rule.violated(context("X", mapOf("cycle_from" to number(2025)))), "a half-filled cycle judges nothing yet")
        assertFalse(rule.violated(context("X", mapOf("cycle_from" to number(2025), "cycle_to" to number(2027)))), "no leave date yet judges nothing")
    }

    @Test
    fun furloughIsNotGrantedToNccOrReEmployedOfficers() {
        // why: p.119 note 7 - furlough is not granted to NCC officers and re-employed officers, so the order would be rejected.
        val rule = check(LongLeaveRules.FURLOUGH_NOT_NCC)
        assertTrue(rule.violated(context("X", answers = setOf(LeaveRules.ANSWER_NCC_REEMPLOYED))))
        assertFalse(rule.violated(context("X")))
        assertFalse(rule.violated(context("X", answers = setOf(LeaveRules.ANSWER_REEMPLOYED_SAME_YEAR))), "that answer alone is not the NCC answer")
    }

    @Test
    fun childCareLeaveIsFiveToThirtyDaysASpellAndSixtyAYear() {
        // why: p.120 note 12.7 (w.e.f. 14 Aug 2019) - not under 5 days, not over 30 days a spell, at most 60 days in a calendar year.
        val rule = check(LongLeaveRules.CCL_LIMITS)

        fun ccl(
            days: Int,
            year: Int,
        ) = context("X", mapOf("ccl_year_days" to number(year)), computed = days(days))
        assertFalse(rule.violated(ccl(5, 5)), "5 days is the smallest spell")
        assertFalse(rule.violated(ccl(30, 60)), "30 days and a year of exactly 60")
        assertTrue(rule.violated(ccl(4, 4)), "under 5 days")
        assertTrue(rule.violated(ccl(31, 31)), "over 30 days")
        assertTrue(rule.violated(ccl(20, 61)), "the year total passes 60")
        assertFalse(rule.violated(context("X", computed = days(10))), "no year total typed yet judges only the spell")
        assertTrue(rule.violated(context("X", mapOf("ccl_year_days" to number(70)))), "the year total alone is enough to breach")
        assertFalse(rule.violated(context("X", mapOf("ccl_year_days" to number(60)))), "60 in the year with no spell worked out yet")
    }

    @Test
    fun terminalLeaveIsTwentyEightDaysOnceAnyAnnualLeaveGrantedWithItIsTakenOut() {
        // why: p.118 prints 28 days; note 10 (p.119) makes its From the first day of the annual leave, so the span is that leave plus 28.
        val rule = check(LongLeaveRules.TERMINAL_LEAVE_28)
        assertFalse(rule.violated(context("X", computed = days(28))), "terminal leave alone: exactly 28")
        assertTrue(rule.violated(context("X", computed = days(27))))
        assertTrue(rule.violated(context("X", computed = days(29))))
        assertFalse(rule.violated(context("X", computed = days(58), neighbours = listOf(neighbour("GTDAL", date(7, 1).date, days(30))))), "30 annual + 28 terminal")
        assertTrue(rule.violated(context("X", computed = days(58), neighbours = listOf(neighbour("GTDAL", date(7, 1).date, days(31))))), "31 annual leaves 27")
        assertFalse(rule.violated(context("X")), "no span worked out yet judges nothing")
    }

    @Test
    fun terminalLeaveBesideAnnualLeaveStartsOnTheFirstDayOfTheAnnualLeave() {
        // why: p.119 note 10 (its "5.8" is the stale number of TERLEV) - in this case the From date is the date annual leave commences.
        val rule = check(LongLeaveRules.TERMINAL_WITH_ANNUAL)

        fun terminal(from: Int) = context("X", values = mapOf("leave_from" to date(7, from)), neighbours = listOf(neighbour("GTDAL", date(7, 1).date, days(30))))
        assertFalse(rule.violated(terminal(1)), "starts with the annual leave")
        assertTrue(rule.violated(terminal(31)), "starts after it, as a stand-alone grant would")
        assertFalse(rule.violated(context("X", mapOf("leave_from" to date(7, 31)))), "no annual leave beside it: nothing to match")
        assertFalse(rule.violated(context("X", neighbours = listOf(neighbour("GTDAL", date(7, 1).date)))), "no terminal From typed yet judges nothing")
        assertFalse(rule.violated(context("X", mapOf("leave_from" to date(7, 1)), neighbours = listOf(neighbour("GTDAL", null)))), "annual leave not dated yet judges nothing")
        val extended = listOf(neighbour("EXTNAL", date(7, 31).date, days(5)), neighbour("GTDAL", date(7, 1).date, days(30)))
        assertFalse(rule.violated(context("X", mapOf("leave_from" to date(7, 1)), neighbours = extended)), "the earliest of several annual grants is the first day of the annual leave")
        assertTrue(rule.violated(context("X", mapOf("leave_from" to date(7, 31)), neighbours = extended)), "the extension's own first day is not")
    }

    @Test
    fun aFixedLengthLeaveMustSpanItsFixedLengthOfDays() {
        // why: p.122 and p.124 print "180 days" and "15 days" in the narrative, so dates that do not span them contradict the order's own words.
        for ((id, fixed) in listOf(LongLeaveRules.ADOPTION_LEAVE_180 to 180, LongLeaveRules.PATERNITY_ADOPTION_15 to 15)) {
            val rule = check(id)
            assertFalse(rule.violated(context("X", computed = days(fixed))), "$id: exactly $fixed")
            assertTrue(rule.violated(context("X", computed = days(fixed - 1))), "$id: one short")
            assertTrue(rule.violated(context("X", computed = days(fixed + 1))), "$id: one over")
            assertFalse(rule.violated(context("X")), "$id: no span yet")
        }
    }

    @Test
    fun anOrderThatNamesTheLeaveItBuildsOnIsWarnedAboutWhileItIsBlank() {
        // why: pp.129-130 - the follow-up quotes the order that granted or declared the earlier occurrence; a blank is normal while it is unpublished.
        for ((id, slot) in listOf(
            LongLeaveRules.GRANT_QUOTED_FURLOUGH to "grant",
            LongLeaveRules.GRANT_QUOTED_STUDY to "grant",
            LongLeaveRules.GRANT_QUOTED_OVERSTAY to "grant",
            LongLeaveRules.DECLARED_QUOTED_OVERSTAY to "declared",
            LongLeaveRules.DECLARED_QUOTED_AWL to "declared",
        )) {
            val rule = check(id)
            assertTrue(rule.violated(context("X")), "$id: nothing quoted")
            assertTrue(rule.violated(context("X", mapOf(slot to number(1)))), "$id: a number is not an order")
            assertFalse(rule.violated(context("X", mapOf(slot to ref("0001/26")))), "$id: quoted")
        }
        assertTrue(check(LongLeaveRules.GRANT_QUOTED_STUDY).violated(context("X", mapOf("declared" to ref("0001/26")))), "the study rules read the grant slot only")
    }

    @Test
    fun anExtensionOrReturnThatMovedTheEarlierAllowanceDatesPointsAtParaEleven() {
        // why: para 11 p.8 - an order cannot be amended, so the earlier ration and R&H rows that ran to the old return day are cancelled and notified afresh.
        val rule = check(LongLeaveRules.REISSUE_ALLOWANCES)
        assertTrue(rule.violated(context("X", answers = setOf(LongLeaveRules.ANSWER_EARLIER_ALLOWANCES))))
        assertFalse(rule.violated(context("X")))
        assertFalse(rule.violated(context("X", answers = setOf("ration"))), "the ration answer is a different question")
    }
}
