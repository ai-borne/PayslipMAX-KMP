package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.Part2Slot
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
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.STOP
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
 * The stop template carries a date (the day back) and a station (an area detail) so a test can tell the two apart: since phase 9 a date
 * is never brought back, because the sanction period of a field-area grant would otherwise carry last time's end date into this one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftPrefillTest : LeaveDraftTestBase() {
    private val hillCode = AnswerQuestion.OneOf("hill_code", "hill_code:", listOf("H1", "H2"), setOf(IN_HILL_STATION))

    private val lookup =
        SyntheticPart2Templates.lookup.let { base ->
            base.copy(templates = base.templates.map { if (it.code == STOP) it.copy(slots = it.slots + Part2Slot("station", "text", "Station", "The station."), narrative = it.narrative + " at {station}") else it })
        }

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

    private fun station(name: String) = SlotValue.TextValue(name)

    private suspend fun earlier(
        id: String,
        updatedAt: Long,
        choice: String,
        values: Map<String, SlotValue>,
    ) {
        val request = OccurrenceRequest(GRANT, values, setOf("hill_code:$choice"))
        repo.saveDraft(Part2Draft(id, LeaveEventInput(listOf(request)), emptyMap(), 1L, updatedAt))
    }

    private fun TestScope.fresh(id: String = "new") = LeaveDraftViewModel(id, lookup, repo, LeaveEventEditor.start(GRANT), rules, { clock }, StandardTestDispatcher(testScheduler))

    private fun LeaveDraftViewModel.value(slot: String) = state.value.sections[0].fields.firstOrNull { it.slotId == slot }?.value

    @Test
    fun aChoiceFillsWhatItNewlyAsksFromTheNewestOtherDraftThatMadeTheSameChoice() =
        runTest {
            earlier("old", 10L, "H1", mapOf("station" to station("OLD1")))
            earlier("newest", 20L, "H1", mapOf("station" to station("NEW1")))
            earlier("other-choice", 30L, "H2", mapOf("station" to station("OTHER")))
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Words("NEW1"), vm.value("station"), "the newest draft with the same choice, not a newer one that chose differently")
        }

    @Test
    fun aDateTheChoiceAddsIsNeverBroughtBackBecauseItWouldBeLastTimesDateQuietlyKept() =
        runTest {
            earlier("old", 10L, "H1", mapOf("rejoin" to day(9, 5), "station" to station("OLD1")))
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Words("OLD1"), vm.value("station"), "the detail beside the date is brought back")
            assertEquals(FieldValue.Date(null), vm.value("rejoin"), "the date is the officer's to enter again")
            assertEquals(null, saved("new")!!.event.requests.single().values["rejoin"], "and none was stored behind their back")
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
            earlier("old", 10L, "H1", mapOf("station" to station("OLD1")))
            val vm = fresh()
            vm.setText(0, "station", "TYPED")
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Words("TYPED"), vm.value("station"))
        }

    @Test
    fun theDraftItselfIsNeverItsOwnSourceAndClearingTheChoiceCopiesNothing() =
        runTest {
            earlier("new", 10L, "H1", mapOf("station" to station("OLD1")))
            val vm = fresh("new")
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Words(""), vm.value("station"), "the saved copy of this very draft is not another draft")
            earlier("old", 5L, "H1", mapOf("station" to station("OLD1")))
            vm.choose(0, "hill_code", null)
            advanceUntilIdle()
            assertEquals(null, vm.value("station"), "no choice, no field, nothing copied")
        }

    @Test
    fun noEarlierDraftMeansNothingIsFilledAndTheChoiceStillStands() =
        runTest {
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(FieldValue.Words(""), vm.value("station"))
            assertEquals("H1", vm.state.value.sections[0].questions.first { it.id == "hill_code" }.chosen)
        }

    @Test
    fun aPrefilledValueIsSavedLikeAnyOtherAnswer() =
        runTest {
            earlier("old", 10L, "H1", mapOf("station" to station("OLD1")))
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            advanceUntilIdle()
            assertEquals(SlotValue.TextValue("OLD1"), saved("new")!!.event.requests.single().values["station"])
        }

    @Test
    fun aChoiceClearedBeforeTheCopyLandsIsNotFilledIn() =
        runTest {
            earlier("old", 10L, "H1", mapOf("station" to station("OLD1")))
            val vm = fresh()
            vm.choose(0, "hill_code", "H1")
            vm.choose(0, "hill_code", null)
            advanceUntilIdle()
            assertEquals(null, vm.value("station"), "the choice is gone, so the input it added is gone")
            assertEquals(emptyMap(), saved("new")!!.event.requests.single().values, "and no value for it was stored behind the officer's back")
        }
}
