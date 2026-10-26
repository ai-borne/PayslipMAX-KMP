package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.Ch5Answers.request
import org.junit.Test
import kotlin.test.assertEquals

/**
 * What the leave form asks for the Chapter 5 codes, on the real bundle (Part II plan, phase 8): the questions that feed the new checks are
 * asked for exactly the codes whose templates run those checks, and the slots are the ones the manual's columns need.
 */
class Part2BatchThreeFormTest {
    private val planner = LeaveFormPlanner(RealPart2.engine, Part2Rules.shipped())

    private fun asked(code: String) = planner.questions(code).map { it.id }

    private fun fields(
        code: String,
        vararg answers: String,
    ) = planner.fields(request(code, emptyMap(), *answers)).map { it.slot.id }

    @Test
    fun annualLeaveAsksTheNccAndReEmployedQuestionsBesideRationAndRh() {
        assertEquals(listOf("ration", "rh_code", "ncc_reemployed", "reemployed_same_year"), asked("GTDAL"))
        assertEquals(listOf("ration", "rh_code"), asked("GTDALCF"), "a carry-forward has no 60-day check (Round 12)")
    }

    @Test
    fun furloughAsksRationAndTheNccQuestionButNotTheSameYearOne() {
        assertEquals(listOf("ration", "ncc_reemployed"), asked("FURLEV"))
    }

    @Test
    fun theRationQuestionIsAskedForLongLeavesToo() {
        for (code in listOf("MTRNLV", "SPMTRNLV", "GTDCCL", "CHADPLVE", "STDLEV")) assertEquals(listOf("ration"), asked(code), code)
        for (code in listOf("PTRNLV", "PATLCHAD", "EOL", "TERLEV", "OSL", "AWL")) assertEquals(emptyList(), asked(code), code)
    }

    @Test
    fun theFollowUpsThatMoveTheReturnDayAskAboutEarlierAllowances() {
        for (code in listOf("EXTNAL", "VOLREJ", "RECALL", "EXTNFUR")) assertEquals(true, "earlier_allowances" in asked(code), code)
        assertEquals(emptyList(), asked("REJOINAL"), "a plain rejoining asks nothing")
    }

    @Test
    fun theFormAsksForTheCycleAndNeverForTheDayCountOrTheTerminalLeaveDays() {
        assertEquals(listOf("prefix_first", "leave_from", "leave_to", "suffix_last", "cycle_from", "cycle_to"), fields("FURLEV").filter { it != "leave_kind" })
        assertEquals(listOf("leave_from", "leave_to"), fields("TERLEV"), "days is worked out for the check, not asked")
    }
}
