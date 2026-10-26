package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Leave of 180 days or more and the ration four (p.99 note 5): the unit publishes those orders quarterly in arrears, so the leave slip
 * carries none and says why. Each test states what goes wrong without the rule: an overpayment of ration allowance.
 */
class RationLongLeaveTest {
    private val ration = setOf(RationRules.ANSWER_ON_RATION)

    private fun days(count: Int) = mapOf("days" to number(count))

    private fun condition() = assertNotNull(RationRules.all.filterIsInstance<ConditionRule>().firstOrNull { it.id == RationRules.ON_RATION_UNDER_180 })

    private fun note() = assertNotNull(RationRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == RationRules.RATION_LONG_LEAVE })

    @Test
    fun theFamilyHoldsTheTwoOldAndTheTwoNewRules() {
        assertEquals(setOf("on_ration", "on_ration_under_180", "ration_long_leave"), RationRules.all.map { it.id }.toSet())
    }

    @Test
    fun theRationFourAreDraftedOnlyForALeaveUnder180DaysOfAnOfficerOnRationStrength() {
        // why: note 5 - leave of 180 days or more is left to the unit's quarterly order, so a slip that drafted it would pay twice.
        assertTrue(condition().holds(context("X", answers = ration, computed = days(179))))
        assertFalse(condition().holds(context("X", answers = ration, computed = days(180))), "180 is the first day of the long leave")
        assertFalse(condition().holds(context("X", computed = days(10))), "not on ration strength")
        assertTrue(condition().holds(context("X", answers = ration)), "no days worked out yet: the short case, as on_ration alone")
    }

    @Test
    fun theLongLeaveNoteIsShownOnlyToAnOfficerOnRationStrengthWithAtLeast180Days() {
        // why: an officer not on ration strength has no ration order to leave out, so the note would only confuse.
        assertTrue(note().violated(context("X", answers = ration, computed = days(180))))
        assertTrue(note().violated(context("X", answers = ration, computed = days(365))))
        assertFalse(note().violated(context("X", answers = ration, computed = days(179))))
        assertFalse(note().violated(context("X", computed = days(180))), "not on ration strength")
        assertFalse(note().violated(context("X", answers = ration)), "no days worked out yet")
    }

    @Test
    fun theRationQuestionIsAskedForTheConditionsAndTheNoteAlike() {
        // why: the form asks "Are you on ration strength?" exactly when a link or a check reads the answer.
        val question = RationRules.questions.single()
        assertEquals(setOf(RationRules.ON_RATION, RationRules.ON_RATION_UNDER_180, RationRules.RATION_LONG_LEAVE), question.rules)
    }
}
