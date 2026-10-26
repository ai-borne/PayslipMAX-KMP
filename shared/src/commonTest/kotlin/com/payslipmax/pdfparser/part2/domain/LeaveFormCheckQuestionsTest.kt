package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NO_TEMPLATE
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ON_RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.RATION
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A question a check reads (the NCC and re-employed answers of the annual-leave cap, the "earlier allowances" answer of an extension)
 * is asked for exactly the codes whose templates run that check, whatever the answers so far (Phase 8). Before, only a link's
 * condition could ask a question. The templates and rules are invented: GRANT runs the cap check and links the ration order,
 * EXTENSION runs only a quoting check, RATION runs and links nothing.
 */
class LeaveFormCheckQuestionsTest {
    private val byCheck = AnswerQuestion.YesNo("read_by_cap", "cap_answer", setOf(CAP_DAYS))
    private val byCondition = AnswerQuestion.YesNo("read_by_link", "link_answer", setOf(ON_RATION))
    private val unread = AnswerQuestion.YesNo("unread", "x", setOf("a_rule_nobody_runs"))
    private val rules = Part2Rules(emptyList(), listOf(byCheck, byCondition, unread))
    private val planner = LeaveFormPlanner(Part2DraftEngine(SyntheticPart2Templates.lookup, rules), rules)

    private fun asked(code: String) = planner.questions(code).map { it.id }

    @Test
    fun aQuestionIsAskedForACodeWhoseTemplateRunsTheCheckItsAnswerFeeds() {
        assertEquals(listOf("read_by_cap", "read_by_link"), asked(GRANT), "the check on the template and the condition on its link both ask")
    }

    @Test
    fun aCheckOnATemplateTheCodeNeverReachesAsksNothing() {
        assertEquals(emptyList(), asked(EXTENSION), "EXTENSION runs only the quoting check and links nothing")
        assertEquals(emptyList(), asked(RATION), "RATION runs no check")
        assertEquals(emptyList(), asked(NO_TEMPLATE), "a code with no template")
    }
}
