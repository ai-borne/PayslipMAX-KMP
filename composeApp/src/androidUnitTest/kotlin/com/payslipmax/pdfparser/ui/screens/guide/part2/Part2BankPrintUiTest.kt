package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.print.IndexDocument
import com.payslipmax.pdfparser.part2.print.IndexPrinter
import com.payslipmax.pdfparser.part2.print.PrintOutcome
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.theme.IndexStrings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the officer prints from the Bank with one button. An empty Bank has nothing to print so it offers nothing; a Bank with entries
 * offers it to every officer, lapsed or not (it is their own data, view only); one tap opens the system dialog once under the fixed
 * name; and while the dialog is open the button is off, so it cannot be opened twice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2BankPrintUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }

    private class Printer(val gate: CompletableDeferred<Unit>? = null) : IndexPrinter {
        val jobs = mutableListOf<String>()
        val documents = mutableListOf<IndexDocument>()

        override suspend fun print(
            jobName: String,
            document: IndexDocument,
        ): PrintOutcome {
            jobs += jobName
            documents += document
            gate?.await()
            return PrintOutcome.COMPLETED
        }
    }

    private fun open(
        printer: Printer,
        canAdd: Boolean = true,
    ): GuideNavState = show(GuideNavState(listOf(Part2Destination.Bank)), part2Bank = { bank }, bankAccess = BankAccess(canAdd) {}, indexPrinter = printer)

    private fun seed() = runBlocking { data.repository.saveEntry(SyntheticBank.entry("e1", code = "FIRSTCODE", from = CalendarDate(2026, 4, 1))) }

    @Test
    fun anEmptyBankOffersNothingToPrint() {
        open(Printer())

        count(IndexStrings.printButton).assertCountEquals(0)
    }

    @Test
    fun aBankWithEntriesOffersPrintAndATapPrintsOnceUnderTheFixedName() {
        seed()
        val printer = Printer()
        open(printer)

        composeRule.onNodeWithText(IndexStrings.printButton).assertIsDisplayed().assertIsEnabled()
        tap(IndexStrings.printButton)

        assertEquals(listOf(IndexStrings.jobName), printer.jobs)
        assertEquals(1, printer.documents.single().years.single().rows.size)
    }

    @Test
    fun aLapsedOfficerCanStillPrintWhatTheyHold() {
        seed()
        val printer = Printer()
        open(printer, canAdd = false)

        tap(IndexStrings.printButton)

        assertEquals(1, printer.jobs.size)
    }

    @Test
    fun theButtonIsOffWhileTheDialogIsOpenSoItCannotBeOpenedTwice() {
        seed()
        val gate = CompletableDeferred<Unit>()
        val printer = Printer(gate)
        open(printer)

        tap(IndexStrings.printButton)
        composeRule.onNodeWithText(IndexStrings.printButton).assertIsNotEnabled()
        tap(IndexStrings.printButton)
        assertEquals(1, printer.jobs.size)

        gate.complete(Unit)
        settle()
        composeRule.onNodeWithText(IndexStrings.printButton).assertIsEnabled()
    }
}
