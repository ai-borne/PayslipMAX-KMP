package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.FillNote
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.YearCheck
import com.payslipmax.pdfparser.part2.leavebank.CrossCheck
import com.payslipmax.pdfparser.testing.FakeLeaveBankSource
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.theme.Part2LeaveBankStrings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The fill when the Leave Bank is not instant (plan, phase 8b): the fill is applied to the officer's answers at the moment the Leave Bank
 * answers, so a slow source never overwrites what was typed meanwhile, a closed form is never written to, and the notes and the check follow
 * the rows as they change. FAKES THAT ANSWER INSTANTLY HIDE THESE BUGS, so the source here really suspends.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LeaveDraftFillLateTest : LeaveDraftFillBase() {
    @Test
    fun aLeaveBankThatReallySuspendsShowsWaitingAndFillsWhenItAnswers() =
        runTest {
            val source = FakeLeaveBankSource(initial = null)
            val vm = fillModel(source)
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertEquals(Part2LeaveBankStrings.waiting, vm.message())
            assertEquals(FieldValue.Digits(""), vm.value("year"))
            source.publish(rows)
            advanceUntilIdle()
            assertEquals(FieldValue.Digits("2024"), vm.value("year"))
            assertEquals(Part2LeaveBankStrings.filled(2), vm.message())
        }

    @Test
    fun aLeaveBankThatAnswersAfterTheOfficerTypedFillsOnlyWhatIsStillEmptyFromTheAnswersAtThatMoment() =
        runTest {
            val source = FakeLeaveBankSource(initial = null)
            val vm = fillModel(source)
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            vm.setDate(0, "leave_to", CalendarDate(2030, 9, 4))
            vm.setDate(0, "leave_from", CalendarDate(2026, 7, 5))
            source.publish(rows)
            advanceUntilIdle()
            assertEquals(FieldValue.Digits("2030"), vm.value("year"), "worked out from the day typed while the Leave Bank was slow")
            assertEquals(FieldValue.Date(CalendarDate(2026, 7, 5)), vm.value("leave_from"), "typed meanwhile, so kept")
            assertEquals(Part2LeaveBankStrings.filled(1), vm.message())
        }

    @Test
    fun aFormClosedWhileTheLeaveBankIsStillSilentIsNeverWrittenToAndTheListeningStops() =
        runTest {
            val source = FakeLeaveBankSource(initial = null)
            val vm = fillModel(source)
            advanceUntilIdle()
            assertEquals(1, source.listeners, "the open form listens")
            vm.setNumber(0, "year", "2025")
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = true)
            advanceUntilIdle()
            vm.close()
            advanceUntilIdle()
            val saves = repo.saves
            source.publish(rows)
            advanceUntilIdle()
            assertEquals(saves, repo.saves, "no write after close")
            assertEquals(SlotValue.NumberValue(2025), saved()!!.event.requests.single().values["year"], "what was typed is what is stored, not what the late answer would have filled")
            assertEquals(0, source.listeners, "closing stops the listening")
        }

    @Test
    fun anOccurrenceRemovedWhileTheLeaveBankIsSilentTakesItsWaitingFillWithItNotPassedToTheOneThatSlidIntoItsPlace() =
        runTest {
            // why: the press belongs to the occurrence it was made on; removing an earlier one moves the next into that index.
            val source = FakeLeaveBankSource(initial = null)
            val vm = fillModel(source, start = LeaveEventInput(listOf(OccurrenceRequest(GRANT), OccurrenceRequest(GRANT))))
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertEquals(Part2LeaveBankStrings.waiting, vm.message())
            vm.removeOccurrence(0)
            advanceUntilIdle()
            source.publish(rows)
            advanceTimeBy(LeaveDraftViewModel.AUTOSAVE_DELAY_MS + 1)
            advanceUntilIdle()
            assertEquals(emptyList(), fill.asked, "the fill was dropped with its occurrence")
            assertEquals(emptyMap(), saved()!!.event.requests.single().values)
            assertNull(vm.message(), "and no stale Waiting is left on the occurrence that took the place")
        }

    @Test
    fun aRemovalTheFormRefusesAndATypedFigureDoNotDropTheWaitingFill() =
        runTest {
            // why: only a real removal moves the places a waiting fill refers to.
            val source = FakeLeaveBankSource(initial = null)
            val vm = fillModel(source)
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = false)
            vm.removeOccurrence(0)
            vm.setDate(0, "leave_to", CalendarDate(2030, 9, 4))
            advanceUntilIdle()
            source.publish(rows)
            advanceUntilIdle()
            assertEquals(FieldValue.Digits("2030"), vm.value("year"), "the only occurrence stays and the fill still lands")
        }

    @Test
    fun aLeaveBankThatAnswersBeforeTheDraftHasLoadedLeavesTheFormLoadingUntilItHas() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val stored = Recording()
            stored.saveDraft(Part2Draft("draft-1", LeaveEventEditor.start(GRANT), emptyMap(), 1L, 2L))
            val slow =
                object : Part2Repository by stored {
                    override suspend fun draft(id: String): Part2Draft? {
                        gate.await()
                        return stored.draft(id)
                    }
                }
            val vm = LeaveDraftViewModel("draft-1", SyntheticPart2Templates.lookup, slow, null, rules, { clock }, StandardTestDispatcher(testScheduler), FakeLeaveBankSource(rows)) { 0L }
            advanceUntilIdle()
            assertEquals(DraftStatus.LOADING, vm.state.value.status, "rows alone are not a reason to draw a form with no answers")
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(DraftStatus.READY, vm.state.value.status)
        }

    @Test
    fun aSecondPressWhileWaitingReplacesTheFirstSoTheFillRunsOnce() =
        runTest {
            val source = FakeLeaveBankSource(initial = null)
            val vm = fillModel(source)
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = false)
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            source.publish(rows)
            advanceUntilIdle()
            assertEquals(1, fill.asked.size)
            assertEquals(Part2LeaveBankStrings.filled(2), vm.message())
        }

    @Test
    fun theFillIsHandedTheLeaveBankRowsAndTheOfficersOtherDraftsButNotThisOne() =
        runTest {
            val other = Part2Draft("other", LeaveEventEditor.start(EXTENSION), emptyMap(), 1L, 5L)
            repo.saveDraft(other)
            val vm = fillModel()
            advanceUntilIdle()
            vm.setNumber(0, "year", "2025")
            advanceTimeBy(LeaveDraftViewModel.AUTOSAVE_DELAY_MS + 1)
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = true)
            advanceUntilIdle()
            val inputs = fill.asked.single()
            assertEquals(rows, inputs.rows)
            assertEquals(listOf(EXTENSION), inputs.drafted.map { it.code })
            assertEquals(emptyList(), inputs.printed, "the other draft was never printed")
        }

    @Test
    fun theFilledFiguresAreSavedLikeAnyOtherAnswer() =
        runTest {
            val vm = fillModel()
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = false)
            advanceTimeBy(LeaveDraftViewModel.AUTOSAVE_DELAY_MS + 1)
            advanceUntilIdle()
            assertEquals(SlotValue.NumberValue(2024), saved()!!.event.requests.single().values["year"])
        }

    @Test
    fun anIndexOutsideTheEventOrACodeWithNoFillDoesNothing() =
        runTest {
            val vm = fillModel()
            advanceUntilIdle()
            vm.fillFromLeaveBank(3, replace = false)
            vm.fillFromLeaveBank(-1, replace = true)
            val extension = fillModel(start = LeaveEventEditor.start(EXTENSION))
            advanceUntilIdle()
            extension.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertEquals(emptyList(), fill.asked)
            assertNull(extension.message())
        }

    @Test
    fun theNotesAndTheCheckAgainstTheStatementFollowTheRowsAsTheyChange() =
        runTest {
            val source = FakeLeaveBankSource(rows)
            val vm = fillModel(source, start = complete())
            advanceUntilIdle()
            val first = vm.state.value
            assertEquals(listOf(Part2LeaveBankStrings.note(FillNote.StatementTotal(1, 0))), first.sections[0].fill?.notes)
            assertEquals(listOf(Part2LeaveBankStrings.crossCheck(YearCheck(2024, CrossCheck.Matches(1)))), first.slip?.crossChecks)
            source.publish(rows + accrued("2024-12-31", 19, 301))
            advanceUntilIdle()
            assertEquals(listOf(Part2LeaveBankStrings.crossCheck(YearCheck(2024, CrossCheck.Matches(2)))), vm.state.value.slip?.crossChecks)
            assertEquals(listOf(Part2LeaveBankStrings.note(FillNote.StatementTotal(2, 0))), vm.state.value.sections[0].fill?.notes)
        }

    @Test
    fun noCheckAgainstTheStatementIsDrawnWhereTheLeaveBankIsOffOrTheCodeHasNone() =
        runTest {
            assertEquals(emptyList(), fillModel(source = null, start = complete()).also { advanceUntilIdle() }.state.value.slip?.crossChecks)
            fill.check = { _, _ -> null }
            assertEquals(emptyList(), fillModel(start = complete()).also { advanceUntilIdle() }.state.value.slip?.crossChecks)
        }
}
