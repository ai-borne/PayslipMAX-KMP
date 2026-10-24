package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.print.PrintOutcome
import com.payslipmax.pdfparser.testing.FakeSlipPrinter
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: what the clerk was handed is recorded only when the system says it was printed, because that time starts the reminder to add the
 * published order (Phase 7a). A cancelled dialog, a slip with an unticked check that the manual says will be rejected, or an
 * unfinished slip must never count; editing afterwards must not erase the record (Round 18); and nothing is written until the printer answers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftPrintTest : LeaveDraftTestBase() {
    private val printer = FakeSlipPrinter()

    /** An extension with no grant order quoted: the manual says it will be rejected, so its check is an `ack`. */
    private val needsAck =
        LeaveEventInput(
            listOf(
                OccurrenceRequest(EXTENSION, mapOf("ext_from" to SlotValue.DateValue(CalendarDate(2026, 9, 5)), "new_to" to SlotValue.DateValue(CalendarDate(2026, 9, 20)))),
            ),
        )

    private fun LeaveDraftViewModel.ackKey() = state.value.slip!!.findings.first { it.ack }.key

    @Test
    fun printingHandsThePrinterTheSlipsDocumentUnderTheFixedJobName() =
        runTest {
            val vm = model(start = complete())

            vm.print(printer)
            advanceUntilIdle()

            assertEquals(listOf(Part2Strings.printJobName), printer.jobNames)
            assertEquals(vm.state.value.slip!!.toDocument(), printer.documents.single())
        }

    @Test
    fun nothingIsRecordedUntilThePrinterReportsCompletionAndThenAtOnce() =
        runTest {
            val vm = model(start = complete())
            runCurrent()
            printer.hold()

            vm.print(printer)
            assertTrue(vm.state.value.printing, "the dialog is up, shown at once and not at the next unrelated change")
            assertNull(vm.state.value.printed)
            assertEquals(0, repo.saves)
            assertNull(saved())

            clock = 4_000L
            printer.release()
            runCurrent()

            val snapshot = assertNotNull(saved()?.printed, "saved at once, not after the autosave pause")
            assertEquals(4_000L, snapshot.at)
            assertEquals(mapOf(GRANT to "a0000001"), snapshot.revs)
            assertEquals(listOf(GRANT), snapshot.rows.map { it.code })
            assertEquals(Part2Strings.printedOnLine("01/01/1970"), vm.state.value.printed!!.line)
            assertNull(vm.state.value.printed!!.note)
            assertTrue(!vm.state.value.printing)
        }

    @Test
    fun aPrintThatIsNotPrintedRecordsNothing() =
        runTest {
            val vm = model(start = complete())
            printer.outcome = PrintOutcome.NOT_PRINTED

            vm.print(printer)
            advanceUntilIdle()

            assertEquals(1, printer.jobNames.size)
            assertNull(vm.state.value.printed)
            assertEquals(0, repo.saves)
            assertTrue(!vm.state.value.printing, "the officer can print again")
        }

    @Test
    fun anAckCheckStopsPrintingUntilItIsTicked() =
        runTest {
            val vm = model(start = needsAck)
            assertTrue(!vm.state.value.slip!!.readyToPrint)

            vm.print(printer)
            advanceUntilIdle()
            assertTrue(printer.jobNames.isEmpty(), "the manual says this order will be rejected: not before the officer has read it")

            vm.acknowledge(vm.ackKey(), true)
            vm.print(printer)
            advanceUntilIdle()
            assertEquals(1, printer.jobNames.size)
            assertNotNull(saved()?.printed)
        }

    @Test
    fun anUnfinishedOrMissingDraftIsNeverPrinted() =
        runTest {
            model(start = LeaveEventEditor.start(GRANT)).print(printer)
            model(start = null, id = "gone").print(printer)
            advanceUntilIdle()

            assertTrue(printer.jobNames.isEmpty())
        }

    @Test
    fun aDraftThatIsBeingDeletedIsNotPrinted() =
        runTest {
            val vm = model(start = complete())
            vm.setNumber(0, "year", "2026")
            advanceUntilIdle()

            vm.delete { }
            vm.print(printer)
            advanceUntilIdle()

            assertTrue(printer.jobNames.isEmpty())
        }

    @Test
    fun aSecondPrintWhileTheDialogIsOpenIsIgnored() =
        runTest {
            val vm = model(start = complete())
            printer.hold()

            vm.print(printer)
            runCurrent()
            vm.print(printer)
            runCurrent()

            assertEquals(1, printer.jobNames.size)
        }

    @Test
    fun aModelThatIsClosedPrintsNothingAndRecordsNothingForADialogStillOpen() =
        runTest {
            val vm = model(start = complete())
            printer.hold()
            vm.print(printer)
            runCurrent()

            vm.close()
            printer.release()
            advanceUntilIdle()
            assertEquals(0, repo.saves, "the officer left the draft: a late answer must not write it back")

            vm.print(printer)
            advanceUntilIdle()
            assertEquals(1, printer.jobNames.size)
        }

    @Test
    fun aDraftDeletedWhileTheDialogIsOpenIsNotWrittenBack() =
        runTest {
            val vm = model(start = complete())
            vm.setNumber(0, "year", "2026")
            advanceUntilIdle()
            printer.hold()
            vm.print(printer)
            runCurrent()

            var gone = false
            vm.delete { gone = true }
            printer.release()
            advanceUntilIdle()

            assertTrue(gone)
            assertNull(saved(), "a print that completes after the officer deleted the draft must not bring it back")
        }
}
