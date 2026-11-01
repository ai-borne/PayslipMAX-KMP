package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.print.IndexDocument
import com.payslipmax.pdfparser.part2.print.IndexPrinter
import com.payslipmax.pdfparser.part2.print.PrintOutcome
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.theme.IndexStrings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the officer prints the WHOLE Bank, whatever chip is chosen, through the system dialog, and the print leaves no trace: nothing is
 * written whether the job is printed, cancelled or fails, the job name never carries what was typed, an empty Bank never reaches the
 * printer, and a second tap while the dialog is open does not open a second dialog.
 */
class IndexPrintingTest : BankTestBase() {
    private class RecordingPrinter(private val outcome: PrintOutcome = PrintOutcome.COMPLETED, private val gate: CompletableDeferred<Unit>? = null) : IndexPrinter {
        val jobs = mutableListOf<String>()
        val documents = mutableListOf<IndexDocument>()

        override suspend fun print(
            jobName: String,
            document: IndexDocument,
        ): PrintOutcome {
            jobs += jobName
            documents += document
            gate?.await()
            return outcome
        }
    }

    private class ThrowingPrinter : IndexPrinter {
        override suspend fun print(
            jobName: String,
            document: IndexDocument,
        ): PrintOutcome = throw IllegalStateException("the print service died")
    }

    private suspend fun seed() {
        repository.saveOrder(SyntheticBank.order("o1"))
        repository.saveEntry(SyntheticBank.entry("waiting", code = "W1", from = CalendarDate(2026, 4, 1)))
        repository.saveEntry(SyntheticBank.entry("done", code = "D1", orderId = "o1", from = CalendarDate(2025, 5, 1)))
    }

    @Test
    fun theWholeBankIsPrintedWhateverChipIsChosenUnderTheFixedJobName() =
        runTest {
            seed()
            val model = bank()
            model.setFilter(com.payslipmax.pdfparser.part2.domain.BankFilter.NOT_ATTACHED)
            val printer = RecordingPrinter()

            model.indexPrint.print(printer)

            assertEquals(listOf(IndexStrings.jobName), printer.jobs)
            assertEquals(listOf("W1", "D1"), printer.documents.single().years.flatMap { it.rows }.map { it.headline.substringBefore(" ·") })
        }

    @Test
    fun theJobNameIsTheSameWhateverTheBankHolds() =
        runTest {
            val first = RecordingPrinter()
            seed()
            bank().indexPrint.print(first)
            repository.saveEntry(SyntheticBank.entry("more", code = "M1", from = CalendarDate(2024, 1, 1)))
            val second = RecordingPrinter()
            bank().indexPrint.print(second)

            assertEquals(first.jobs, second.jobs)
            assertTrue(first.jobs.single().none { it.isDigit() }, "no date, count or number in the name the system keeps")
        }

    @Test
    fun anEmptyBankNeverReachesThePrinter() =
        runTest {
            val printer = RecordingPrinter()

            bank().indexPrint.print(printer)

            assertEquals(emptyList(), printer.jobs)
            assertFalse(bank().indexPrint.printing.value)
        }

    @Test
    fun aCancelledOrFailedJobAndACompletedOneLeaveNoRecordAnywhere() =
        runTest {
            seed()
            val model = bank()
            val before = model.state.first { it.loaded }
            val entries = repository.observeEntries().first().items
            val orders = repository.observeOrders().first().items

            for (outcome in PrintOutcome.entries) model.indexPrint.print(RecordingPrinter(outcome))
            model.indexPrint.print(ThrowingPrinter())

            assertEquals(entries, repository.observeEntries().first().items, "no entry was touched")
            assertEquals(orders, repository.observeOrders().first().items, "no order was touched")
            assertEquals(before.lastBackup, model.state.first { it.loaded }.lastBackup, "a print is not a backup")
            assertEquals(null, lastBackup.load())
        }

    @Test
    fun aSecondTapWhileTheDialogIsOpenDoesNotOpenAnotherAndTheFlagClearsAtTheEnd() =
        runTest {
            seed()
            val gate = CompletableDeferred<Unit>()
            val printer = RecordingPrinter(gate = gate)
            val model = bank()

            model.indexPrint.print(printer)
            assertTrue(model.indexPrint.printing.value, "the button is disabled while the dialog is open")
            model.indexPrint.print(printer)
            assertEquals(1, printer.jobs.size)

            gate.complete(Unit)
            assertFalse(model.indexPrint.printing.value)
        }

    @Test
    fun aPrinterThatFailsLeavesTheButtonUsableAgain() =
        runTest {
            seed()
            val model = bank()

            model.indexPrint.print(ThrowingPrinter())

            assertFalse(model.indexPrint.printing.value)
            val printer = RecordingPrinter()
            model.indexPrint.print(printer)
            assertEquals(1, printer.jobs.size)
        }
}
