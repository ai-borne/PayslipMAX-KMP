package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The draft a leave form keeps: it is saved as the officer goes (one write after a pause, never one per key), restored by id, flushed
 * when the form is left, and never resurrected after a delete. The templates are invented, so these tests pin the lifecycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftViewModelTest : LeaveDraftTestBase() {
    @Test
    fun aNewDraftShowsItsFormAtOnceAndWritesNothingUntilTheOfficerChangesSomething() =
        runTest {
            val vm = model()
            assertEquals(DraftStatus.READY, vm.state.value.status)
            assertEquals(GRANT, vm.state.value.sections.single().code)
            advanceUntilIdle()
            assertEquals(0, repo.saves)
            assertNull(saved())
        }

    @Test
    fun theFirstChangeIsSavedAfterThePauseAndNotBefore() =
        runTest {
            val vm = model()
            vm.setDate(0, "leave_from", from)
            advanceTimeBy(LeaveDraftViewModel.AUTOSAVE_DELAY_MS - 1)
            runCurrent()
            assertEquals(0, repo.saves)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(1, repo.saves)
            val draft = assertNotNull(saved())
            assertEquals(SlotValue.DateValue(from), draft.event.requests.single().values["leave_from"])
            assertEquals(mapOf(GRANT to "a0000001"), draft.templateRevs)
            assertEquals(1_000L, draft.createdAt)
            assertEquals(1_000L, draft.updatedAt)
        }

    @Test
    fun aBurstOfChangesIsOneWriteOfTheLastState() =
        runTest {
            val vm = model()
            for (digit in listOf("2", "20", "202", "2026")) {
                vm.setNumber(0, "year", digit)
                advanceTimeBy(LeaveDraftViewModel.AUTOSAVE_DELAY_MS / 3)
            }
            advanceUntilIdle()
            assertEquals(1, repo.saves)
            assertEquals(SlotValue.NumberValue(2026), saved()!!.event.requests.single().values["year"])
        }

    @Test
    fun aLaterChangeIsSavedAgainAndTheDraftKeepsItsCreationTime() =
        runTest {
            val vm = model()
            vm.setNumber(0, "year", "2026")
            advanceUntilIdle()
            clock = 5_000L
            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()
            assertEquals(2, repo.saves)
            assertEquals(1_000L, saved()!!.createdAt)
            assertEquals(5_000L, saved()!!.updatedAt)
        }

    @Test
    fun aChangeThatChangesNothingIsNotSaved() =
        runTest {
            val vm = model()
            vm.setDate(0, "leave_from", from)
            advanceUntilIdle()
            vm.setDate(0, "leave_from", from)
            vm.setYesNo(0, "ration", false)
            advanceUntilIdle()
            assertEquals(1, repo.saves)
        }

    @Test
    fun aDraftIsRestoredFromItsIdWithEveryAnswer() =
        runTest {
            val event = LeaveEventInput(listOf(OccurrenceRequest(GRANT, mapOf("leave_from" to SlotValue.DateValue(from), "year" to SlotValue.NumberValue(2026)), setOf(ANSWER_RATION))))
            repo.saveDraft(Part2Draft("draft-9", event, emptyMap(), 7L, 8L))
            val vm = model(start = null, id = "draft-9")
            assertEquals(DraftStatus.LOADING, vm.state.value.status)
            advanceUntilIdle()
            assertEquals(DraftStatus.READY, vm.state.value.status)
            assertEquals(FieldValue.Date(from), vm.field("leave_from").value)
            assertEquals(FieldValue.Digits("2026"), vm.field("year").value)
            assertTrue(vm.state.value.sections[0].questions.first { it.id == "ration" }.yes)
            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()
            assertEquals(7L, saved("draft-9")!!.createdAt, "an edited draft keeps the time it was created")
        }

    @Test
    fun anIdTheRepositoryNoLongerHoldsIsMissing() =
        runTest {
            val vm = model(start = null, id = "gone")
            advanceUntilIdle()
            assertEquals(DraftStatus.GONE, vm.state.value.status)
            vm.setNumber(0, "year", "2026")
            vm.addOccurrence(GRANT)
            advanceUntilIdle()
            assertEquals(0, repo.saves, "a draft that is gone is never recreated by an edit: adding an occurrence to its empty event would")
            assertNull(saved("gone"))
        }

    @Test
    fun leavingTheFormSavesTheLastChangeAtOnceAndStopsTheModel() =
        runTest {
            val vm = model()
            vm.setDate(0, "leave_from", from)
            vm.close()
            advanceUntilIdle()
            assertEquals(1, repo.saves)
            assertEquals(SlotValue.DateValue(from), saved()!!.event.requests.single().values["leave_from"])
            vm.setNumber(0, "year", "2026")
            advanceUntilIdle()
            assertEquals(1, repo.saves, "a closed model takes no more edits")
        }

    @Test
    fun leavingAFormWithNothingChangedWritesNothing() =
        runTest {
            val vm = model()
            vm.close()
            advanceUntilIdle()
            assertEquals(0, repo.saves)
        }

    @Test
    fun deletingRemovesTheDraftAndNoPendingSaveBringsItBack() =
        runTest {
            val vm = model()
            vm.setDate(0, "leave_from", from)
            advanceUntilIdle()
            vm.setNumber(0, "year", "2026")
            var done = 0
            vm.delete { done++ }
            advanceUntilIdle()
            vm.close()
            advanceUntilIdle()
            assertNull(saved())
            assertEquals(1, done)
            assertEquals(1, repo.saves, "the change pending when the officer deleted is never written")
        }

    @Test
    fun aRefusedWriteIsShownAndAClearedOneIsNot() =
        runTest {
            val vm = model()
            repo.refuse = Part2WriteResult.TOO_MANY
            vm.setNumber(0, "year", "2026")
            advanceUntilIdle()
            assertTrue(vm.state.value.saveFailed)
            repo.refuse = null
            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()
            assertFalse(vm.state.value.saveFailed)
        }

    @Test
    fun theVaultsLastOrderIsQuotedOnTheSlipAndSavedWithTheDraft() =
        runTest {
            val vm = model(complete())
            advanceUntilIdle()
            assertEquals(Part2Strings.lastOrderBlank, vm.state.value.slip?.lastOrder)
            repo.saveOrder(VaultOrder("o1", null, "SYN/9/26", CalendarDate(2026, 3, 1), 1L, 2L))
            advanceUntilIdle()
            assertEquals(Part2Strings.lastOrderLine("SYN/9/26", "01/03/2026"), vm.state.value.slip?.lastOrder)
            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()
            assertEquals(OrderRef("SYN/9/26", CalendarDate(2026, 3, 1)), saved()!!.event.lastOrder)
        }
}
