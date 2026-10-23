package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.AnswerQuestion
import com.payslipmax.pdfparser.part2.domain.ConditionRule
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.RuleContext
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.IN_HILL_STATION
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * "Area details are prefilled from the last draft of the same code, with no profile store" (plan, Architecture; owner, Round 17).
 * Choosing a code a question offers fills the inputs that choice newly asks for from the newest other draft that made the same
 * choice, and nothing else: no date, no answer, nothing already typed. The question is a one-of whose answer the stop's condition reads.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftPrefillTest : LeaveDraftTestBase() {
    private val hillCode = AnswerQuestion.OneOf("hill_code", "hill_code:", listOf("H1", "H2"), setOf(IN_HILL_STATION))

    private val rules: Part2Rules =
        SyntheticPart2Templates.registry().let { base ->
            val hill =
                object : ConditionRule {
                    override val id = IN_HILL_STATION

                    override fun holds(context: RuleContext) = context.request.answers.any { it.startsWith("hill_code:") }
                }
            Part2Rules(base.ids.filter { it != IN_HILL_STATION }.map { base.check(it) ?: checkNotNull(base.condition(it)) } + hill, base.questions + hillCode)
        }

    private fun day(
        m: Int,
        d: Int,
    ) = SlotValue.DateValue(CalendarDate(2026, m, d))

    private suspend fun earlier(
        id: String,
        updatedAt: Long,
        choice: String,
        values: Map<String, SlotValue>,
    ) {
        val request = OccurrenceRequest(GRANT, values, setOf("hill_code:$choice"))
        repo.saveDraft(Part2Draft(id, LeaveEventInput(listOf(request)), emptyMap(), 1L, updatedAt))
    }

    private fun TestScope.fresh(id: String = "new") =
        LeaveDraftViewModel(id, SyntheticPart2Templates.lookup, repo, LeaveEventEditor.start(GRANT), rules, { clock }, StandardTestDispatcher(testScheduler))

    private fun LeaveDraftViewModel.value(slot: String) = state.value.sections[0].fields.firstOrNull { it.slotId == slot }?.value

    @Test
    fun aChoiceFillsWhatItNewlyAsksFromTheNewestOtherDraftThatMadeTheSameChoice() =
        runTest {
            earlier("old", 10L, "H1", mapOf("rejoin" to day(9, 5)))
            earlier("newest", 20L, "H1", mapOf("rejoin" to day(9, 6)))
            earlier("other-choice", 30L, "H2", mapOf("rejoin" to day(9, 7)))
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Date(CalendarDate(2026, 9, 6)), vm.value("rejoin"), "the newest draft with the same choice, not a newer one that chose differently")
        }

    @Test
    fun onlyTheInputsTheChoiceAddsAreFilledNeverDatesTheOfficerAlreadyHasToEnter() =
        runTest {
            earlier("old", 10L, "H1", mapOf("rejoin" to day(9, 5), "leave_from" to day(7, 5), "year" to SlotValue.NumberValue(2025)))
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Date(null), vm.value("leave_from"), "a leave date is never carried over")
            assertEquals(FieldValue.Digits(""), vm.value("year"))
        }

    @Test
    fun anAnswerAlreadyTypedIsNotOverwritten() =
        runTest {
            earlier("old", 10L, "H1", mapOf("rejoin" to day(9, 5)))
            val vm = fresh()
            vm.setDate(0, "rejoin", CalendarDate(2026, 9, 1))
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Date(CalendarDate(2026, 9, 1)), vm.value("rejoin"))
        }

    @Test
    fun theDraftItselfIsNeverItsOwnSourceAndClearingTheChoiceCopiesNothing() =
        runTest {
            earlier("new", 10L, "H1", mapOf("rejoin" to day(9, 5)))
            val vm = fresh("new")
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Date(null), vm.value("rejoin"), "the saved copy of this very draft is not another draft")
            earlier("old", 5L, "H1", mapOf("rejoin" to day(9, 5)))
            vm.choose(0, "hill_code", null)
            advanceUntilIdle()
            assertEquals(null, vm.value("rejoin"), "no choice, no field, nothing copied")
        }

    @Test
    fun noEarlierDraftMeansNothingIsFilledAndTheChoiceStillStands() =
        runTest {
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Date(null), vm.value("rejoin"))
            assertEquals("H1", vm.state.value.sections[0].questions.first { it.id == "hill_code" }.chosen)
        }

    @Test
    fun aPrefilledValueIsSavedLikeAnyOtherAnswer() =
        runTest {
            earlier("old", 10L, "H1", mapOf("rejoin" to day(9, 5)))
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(SlotValue.DateValue(CalendarDate(2026, 9, 5)), saved("new")!!.event.requests.single().values["rejoin"])
        }

    @Test
    fun aChoiceClearedBeforeTheCopyLandsIsNotFilledIn() =
        runTest {
            earlier("old", 10L, "H1", mapOf("rejoin" to day(9, 5)))
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            vm.choose(0, "hill_code", null)
            advanceUntilIdle()
            assertEquals(null, vm.value("rejoin"), "the choice is gone, so the input it added is gone")
            assertEquals(emptyMap(), saved("new")!!.event.requests.single().values, "and no value for it was stored behind the officer's back")
        }
}
