package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 11 item 33 (the Phase 7a hand-off): the year-end reminder for leave year Y is answered by an ACCLVE for Y drafted from the REAL
 * template. Until Phase 8 only a synthetic draft stood in for it, so a template that renamed the code or the `year` slot would have gone
 * unnoticed and the reminder would nag for ever.
 */
class Part2YearEndReminderRealBundleTest : ReminderTestBase() {
    private val printed = at(date(2026, 9, 20))
    private val january = at(date(2027, 1, 15))
    private val grant = leave(date(2026, 10, 5), date(2026, 10, 9), leaveYear = 2026)

    private fun accumulationFor(year: Int) =
        request(
            "ACCLVE",
            "year" to number(year),
            "as_on" to RealPart2.date(12, 31, year),
            "prev_total" to number(100),
            "ltc_days" to number(0),
            "accrued" to number(30),
        )

    private fun yearEnds(vararg drafts: Part2Draft) = plan(drafts.toList(), now = january).of(ReminderKind.YEAR_END)

    @Test
    fun theRealTemplateDraftsWithTheCodeAndTheYearSlotTheReminderRulesNameAndTheReminderEnds() {
        val accumulation = accumulationFor(2026)
        assertTrue(RealPart2.engine.draft(LeaveEventInput(listOf(accumulation))) is DraftResult.Drafted, "the shipped ACCLVE template drafts from these answers")
        assertEquals(ReminderRules.YEAR_END_REQUEST, accumulation.code)
        assertEquals(2026, ReminderRules.yearEndYear(accumulation), "the year slot of the real template is the one the rules read")

        val waiting = yearEnds(draft("grant", grant, printedAt = printed))
        assertEquals(listOf(2026), waiting.map { it.year }, "before the accumulation is drafted the reminder is there")
        assertEquals(emptyList(), yearEnds(draft("grant", grant, printedAt = printed), draft("acclve", accumulationFor(2026))).map { it.year }, "drafted (not even printed): the reminder ends")
    }

    @Test
    fun anAccumulationForAnotherYearAnswersNothing() {
        assertEquals(listOf(2026), yearEnds(draft("grant", grant, printedAt = printed), draft("acclve", accumulationFor(2025))).map { it.year })
    }

    @Test
    fun theShippedTemplateDeclaresExactlyTheSlotTheRulesReadAsANumber() {
        val template = RealPart2.section.templates.single { it.code == ReminderRules.YEAR_END_REQUEST }
        assertEquals("number", template.slots.single { it.id == LeaveRules.YEAR }.kind)
    }
}
