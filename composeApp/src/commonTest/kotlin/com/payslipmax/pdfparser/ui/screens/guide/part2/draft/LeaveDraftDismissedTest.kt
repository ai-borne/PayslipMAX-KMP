package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.print.PrintOutcome
import com.payslipmax.pdfparser.testing.FakeSlipPrinter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * WHY: a dismissal is stored with the draft (DECISIONS Round 26), so the form must not lose it when the officer edits and the draft is saved
 * again; and a print that completes starts the reminders afresh, which is also the way back from a dismissal, so it must clear them, while a
 * print that did not complete must not.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftDismissedTest : LeaveDraftTestBase() {
    private val printer = FakeSlipPrinter()

    private suspend fun heldWithDismissals(): Part2Draft {
        val held = Part2Draft("draft-1", complete(), emptyMap(), 1, 1, dismissed = setOf("return", "add-order"))
        repo.saveDraft(held)
        return held
    }

    @Test
    fun anEditThatIsSavedKeepsWhatWasDismissed() =
        runTest {
            heldWithDismissals()
            val vm = model(start = null)
            runCurrent()

            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()

            assertEquals(setOf("return", "add-order"), saved()!!.dismissed)
        }

    @Test
    fun aPrintThatCompletesClearsThemAndRecordsTheSnapshotInTheSameWrite() =
        runTest {
            heldWithDismissals()
            val vm = model(start = null)
            runCurrent()

            vm.print(printer)
            runCurrent()

            assertEquals(emptySet(), saved()!!.dismissed)
            assertNotNull(saved()!!.printed)
        }

    @Test
    fun aPrintThatDidNotCompleteKeepsThem() =
        runTest {
            heldWithDismissals()
            printer.outcome = PrintOutcome.NOT_PRINTED
            val vm = model(start = null)
            runCurrent()

            vm.print(printer)
            runCurrent()
            vm.setNumber(0, "year", "2027")
            advanceUntilIdle()

            assertEquals(setOf("return", "add-order"), saved()!!.dismissed)
        }
}
