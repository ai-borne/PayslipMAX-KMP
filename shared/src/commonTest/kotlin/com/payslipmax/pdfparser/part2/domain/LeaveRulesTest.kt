package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import com.payslipmax.pdfparser.part2.domain.RuleContexts.date
import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import com.payslipmax.pdfparser.part2.domain.RuleContexts.ref
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The manual's computable leave notes as the checks the officer sees (Part II plan, phase 2b). Each test states why the note matters:
 * a unit that publishes over the annual cap, a long prefix or a casual leave beside another leave has its order queried or rejected.
 * Dates are synthetic.
 */
class LeaveRulesTest {
    private fun check(id: String) = assertNotNull(LeaveRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == id }, "rule $id")

    private fun days(count: Int) = mapOf("days" to number(count))

    @Test
    fun theFamilyOffersExactlyTheChecksTheTemplatesCite() {
        assertEquals(
            setOf("al_year_cap", "al_affix_limit", "cl_combination", "spcl_over_30", "grant_quoted_annual", "grant_quoted_rejoin", "grant_quoted_recall"),
            LeaveRules.all.map { it.id }.toSet(),
        )
        assertTrue(LeaveRules.all.all { it is CheckRule }, "the leave family holds checks only")
    }

    @Test
    fun annualLeaveTakenInTheYearMayNotPassSixtyDaysCountingWhatWasTakenBefore() {
        // why: note 2 p.115 - availed, extended, encashed and adjusted leave together may not exceed 60 days a year.
        val rule = check(LeaveRules.AL_YEAR_CAP)
        assertFalse(rule.violated(context("X", computed = days(60))), "exactly 60 is allowed")
        assertTrue(rule.violated(context("X", computed = days(62))), "note 1's own example: 62 days, not 60")
        assertTrue(rule.violated(context("X", mapOf("earlier_al" to number(40)), computed = days(25))), "40 taken earlier + 25 now is 65")
        assertFalse(rule.violated(context("X", mapOf("earlier_al" to number(40)), computed = days(20))))
        assertFalse(rule.violated(context("X", mapOf("earlier_al" to number(60)))), "no days worked out yet is not a breach")
    }

    @Test
    fun aPrefixAndSuffixTogetherOverFiveDaysAreWarnedAbout() {
        // why: note 9 p.115 - over five days of prefix and suffix, the unit must check the grant.
        val rule = check(LeaveRules.AL_AFFIX_LIMIT)
        val from = "leave_from" to date(7, 10)
        val to = "leave_to" to date(8, 8)
        assertFalse(rule.violated(context("X", mapOf(from, to))), "none")
        assertFalse(rule.violated(context("X", mapOf(from, to, "prefix_first" to date(7, 8), "suffix_last" to date(8, 11)))), "2 + 3 = 5")
        assertTrue(rule.violated(context("X", mapOf(from, to, "prefix_first" to date(7, 7), "suffix_last" to date(8, 11)))), "3 + 3 = 6")
        assertTrue(rule.violated(context("X", mapOf(from, to, "prefix_first" to date(7, 1)))), "a long prefix alone")
        assertFalse(rule.violated(context("X", mapOf(from, to, "prefix_first" to date(7, 20), "suffix_last" to date(8, 1)))), "dates inside the leave add nothing")
    }

    @Test
    fun casualLeaveBesideAnyOtherKindOfLeaveButSpecialCasualLeaveIsAcknowledged() {
        // why: p.117 - casual leave cannot be combined with any other leave except special casual leave.
        val rule = check(LeaveRules.CL_COMBINATION)
        assertFalse(rule.violated(context("GTDCL")), "alone")
        assertFalse(rule.violated(context("GTDCL", others = listOf("GTDSPCL"))), "special casual leave is the allowed partner")
        for (other in listOf("GTDAL", "GTDALCF", "EXTNAL")) {
            assertTrue(rule.violated(context("GTDCL", others = listOf(other))), other)
        }
        assertFalse(rule.violated(context("GTDCL", others = listOf("SORS", "STPRH"))), "a ration or area order is not leave")
    }

    @Test
    fun casualLeaveBesideEveryOtherChapterFiveLeaveIsAcknowledgedToo() {
        // why: Phase 11 item 8 - the check knew only the annual set; p.117 says casual leave cannot join ANY other kind of leave except special casual leave.
        val rule = check(LeaveRules.CL_COMBINATION)
        val others = listOf("FURLEV", "EXTNFUR", "MTRNLV", "SPMTRNLV", "GTDCCL", "EOL", "TERLEV", "CHADPLVE", "PTRNLV", "PATLCHAD", "STDLEV", "STDLVAL", "STDLVFUR")
        for (other in others) assertTrue(rule.violated(context("GTDCL", others = listOf(other))), other)
        assertEquals(13, others.size)
        for (notLeave in listOf("REJOINAL", "VOLREJ", "RECALL", "OSL", "REJOSL", "AWL", "REJAWL", "ACCLVE", "ENCASH", "LVELTC")) {
            assertFalse(rule.violated(context("GTDCL", others = listOf(notLeave))), "$notLeave is not a leave grant")
        }
    }

    @Test
    fun aSpecialCasualLeaveOverThirtyDaysIsWarnedAbout() {
        // why: the row itself reads "upto 30 days" (5.5, p.116); a longer count is almost always a typo in the dates.
        val rule = check(LeaveRules.SPCL_OVER_30)
        assertFalse(rule.violated(context("GTDSPCL", computed = days(30))), "exactly 30 is allowed")
        assertTrue(rule.violated(context("GTDSPCL", computed = days(31))))
        assertFalse(rule.violated(context("GTDSPCL")), "no days worked out yet is not a breach")
    }

    @Test
    fun aFollowUpOrderWithoutTheGrantOrderIsWarnedAboutAndOneQuotingItIsNot() {
        // why: notes 7 (p.115) and 2 (pp.131-132) - the follow-up quotes the order that granted the leave; a blank is normal while it is unpublished.
        for (id in listOf(LeaveRules.GRANT_QUOTED_ANNUAL, LeaveRules.GRANT_QUOTED_REJOIN, LeaveRules.GRANT_QUOTED_RECALL)) {
            val rule = check(id)
            assertTrue(rule.violated(context("X")), "$id: nothing quoted")
            assertFalse(rule.violated(context("X", mapOf("grant" to ref("0001/26")))), "$id: quoted")
        }
    }
}
