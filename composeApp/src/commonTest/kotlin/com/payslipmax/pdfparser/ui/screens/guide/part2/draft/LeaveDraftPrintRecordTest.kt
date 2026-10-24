package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakeSlipPrinter
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the record of what the clerk was handed must survive what happens next: an edit (flagged against it, never erasing it), a second
 * print (replaces it), a restore after process death, a template that moved, a write that fails, and the officer leaving the draft
 * while the record is still being written.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftPrintRecordTest : LeaveDraftTestBase() {
    private val printer = FakeSlipPrinter()

    @Test
    fun anEditStillWaitingToBeSavedAndAPrintThatCompletesAreOneWriteWithBoth() =
        runTest {
            val vm = model(start = complete())
            vm.setNumber(0, "year", "2027")

            vm.print(printer)
            runCurrent()

            assertEquals(1, repo.saves)
            assertEquals(SlotValue.NumberValue(2027), saved()!!.event.requests.single().values["year"])
            assertNotNull(saved()!!.printed)
            advanceUntilIdle()
            assertEquals(1, repo.saves, "the autosave that was waiting is not written again")
        }

    @Test
    fun leavingTheDraftWhileTheRecordIsStillBeingSavedDoesNotLoseIt() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val slow =
                object : Part2Repository by repo {
                    override suspend fun saveDraft(draft: Part2Draft): Part2WriteResult {
                        gate.await()
                        return repo.saveDraft(draft)
                    }
                }
            val vm = LeaveDraftViewModel("draft-1", SyntheticPart2Templates.lookup, slow, complete(), SyntheticPart2Templates.registry(), { clock }, StandardTestDispatcher(testScheduler))
            vm.print(printer)
            runCurrent()

            vm.close()
            gate.complete(Unit)
            advanceUntilIdle()

            assertNotNull(saved()?.printed, "closing flushes what was not yet written, and the record is part of it")
        }

    @Test
    fun editingAfterPrintingKeepsTheSnapshotAndSaysTheDraftHasChanged() =
        runTest {
            val vm = model(start = complete())
            vm.print(printer)
            advanceUntilIdle()
            val printed = saved()!!.printed!!

            clock = 9_000L
            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()

            assertEquals(Part2Strings.changedSincePrinted, vm.state.value.printed!!.note)
            assertEquals(printed, saved()!!.printed, "the record of what the clerk was handed is not touched by an edit")
            assertEquals(9_000L, saved()!!.updatedAt)
        }

    @Test
    fun printingAgainReplacesTheSnapshotWithWhatWasJustHandedOver() =
        runTest {
            val vm = model(start = complete())
            vm.print(printer)
            advanceUntilIdle()
            clock = 9_000L
            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()

            vm.print(printer)
            advanceUntilIdle()

            val printed = saved()!!.printed!!
            assertEquals(9_000L, printed.at)
            assertTrue("2027" in printed.rows.single().narrative)
            assertNull(vm.state.value.printed!!.note)
        }

    @Test
    fun aSavedPrintedDraftIsRestoredWithItsRecord() =
        runTest {
            val event = complete()
            val snapshot = LeaveDraftStateBuilder(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry()).snapshotOf(event, at = 86_400_000L * 3)!!
            repo.saveDraft(Part2Draft("draft-9", event, mapOf(GRANT to "a0000001"), 7L, 8L, snapshot))

            val vm = model(start = null, id = "draft-9")
            advanceUntilIdle()

            assertEquals(Part2Strings.printedOnLine("04/01/1970"), vm.state.value.printed!!.line)
            assertNull(vm.state.value.printed!!.note)
        }

    @Test
    fun aTemplateThatMovedSinceThePrintIsSaidSo() =
        runTest {
            val event = complete()
            val snapshot = PrintedSnapshot.of(requireNotNull(drafted(event)), mapOf(GRANT to "a0000000"), 5L)
            repo.saveDraft(Part2Draft("draft-9", event, mapOf(GRANT to "a0000000"), 7L, 8L, snapshot))

            val vm = model(start = null, id = "draft-9")
            advanceUntilIdle()

            assertEquals(Part2Strings.templateUpdatedSincePrinted, vm.state.value.printed!!.note)
        }

    @Test
    fun aRecordThatCannotBeSavedIsShownAndSavedWithTheNextChange() =
        runTest {
            val vm = model(start = complete())
            repo.refuse = Part2WriteResult.REJECTED

            vm.print(printer)
            advanceUntilIdle()
            assertTrue(vm.state.value.saveFailed)
            assertNotNull(vm.state.value.printed, "it was printed: the officer is told, and the record is kept in memory")

            repo.refuse = null
            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()
            assertNotNull(saved()?.printed, "the next save carries it")
        }

    private fun drafted(event: LeaveEventInput) =
        (
            com.payslipmax.pdfparser.part2.domain.Part2DraftEngine(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry())
                .draft(event) as? com.payslipmax.pdfparser.part2.domain.DraftResult.Drafted
        )?.slip
}
