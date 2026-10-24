package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.print.PrintOutcome
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the officer prints from the slip they have just checked, so the Print button must follow the one gate the manual implies (an
 * unticked `ack` check blocks it), say why when it is blocked, never exist for someone whose Premium ended, record that it was printed
 * only when the system says so, and say afterwards whether the paper still matches the draft. The HRMS check is a read, so it stays.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2PrintUiTest : Part2DraftUiBase() {
    private fun openSlip(
        answers: Boolean = true,
        row: Int = 0,
    ): GuideNavState {
        val nav = show()
        path()
        draftRow(row)
        if (answers) fillLeave(model(nav))
        tap(Part2Strings.seeSlip)
        return nav
    }

    private fun exact(text: String) = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun aReadySlipOffersPrintAndATapPrintsItUnderTheFixedNameAndRecordsIt() {
        openSlip()
        composeRule.onNodeWithText(Part2Strings.printButton).assertIsEnabled()

        tap(Part2Strings.printButton)

        assertEquals(listOf(Part2Strings.printJobName), printer.jobNames)
        see(Part2Strings.printedOnLine("")).assertIsDisplayed()
        assertTrue(!has(Part2Strings.changedSincePrinted))
    }

    @Test
    fun aPrintThatIsCancelledShowsNoPrintedLine() {
        printer.outcome = PrintOutcome.NOT_PRINTED
        openSlip()

        tap(Part2Strings.printButton)

        assertEquals(1, printer.jobNames.size)
        assertTrue(!has(Part2Strings.printedOnLine("")))
        composeRule.onNodeWithText(Part2Strings.printButton).assertIsEnabled()
    }

    @Test
    fun whileTheDialogIsOpenThePrintButtonIsOffAndSaysSo() {
        printer.hold()
        openSlip()

        tap(Part2Strings.printButton)
        assertTrue(has(Part2Strings.printingNow))
        composeRule.onNodeWithText(Part2Strings.printingNow).assertIsNotEnabled()

        printer.release()
        settle()
        assertTrue(has(Part2Strings.printedOnLine("")))
    }

    @Test
    fun anUntickedAckCheckBlocksPrintingAndSaysWhyUntilItIsTicked() {
        val nav = show()
        path()
        draftRow(1)
        model(nav).setDate(0, "ext_from", day(9, 5))
        model(nav).setDate(0, "new_to", day(9, 20))
        settle()
        tap(Part2Strings.seeSlip)

        composeRule.onNodeWithText(Part2Strings.printButton).assertIsNotEnabled()
        see(Part2Strings.printBlocked).assertIsDisplayed()

        tap(Part2Strings.ackTick)
        composeRule.onNodeWithText(Part2Strings.printButton).assertIsEnabled()
        assertTrue(!has(Part2Strings.printBlocked))
    }

    @Test
    fun anOfficerWhosePremiumEndedCanReadTheSlipAndCheckHrmsButNotPrint() {
        savedDraft("old-1")
        val nav = show(unlocked = false, part2Unlocked = false)
        tap(Part2Strings.entryTitle)
        tap(Part2Strings.draftRowTitle(listOf(com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT)))

        assertEquals(Part2Destination.Slip("old-1"), nav.current)
        assertTrue(!has(Part2Strings.printButton))
        see(Part2Strings.hrmsCheckButton).assertIsDisplayed()
    }

    @Test
    fun editingAfterPrintingSaysTheDraftHasChanged() {
        val nav = openSlip()
        tap(Part2Strings.printButton)

        tap(Part2Strings.editDraft)
        model(nav).setNumber(0, "year", "2027")
        settle()
        tap(Part2Strings.seeSlip)

        see(Part2Strings.changedSincePrinted).assertIsDisplayed()
        see(Part2Strings.printedOnLine("")).assertIsDisplayed()
    }

    @Test
    fun theHrmsCheckShowsEveryFieldOfEveryRowAndBackReturnsToTheSlip() {
        val nav = show()
        path()
        draftRow()
        fillLeave(model(nav))
        model(nav).setYesNo(0, "ration", true)
        model(nav).setDate(0, "away_from", day(7, 1))
        settle()
        tap(Part2Strings.seeSlip)

        tap(Part2Strings.hrmsCheckButton)

        assertEquals(Part2Destination.Hrms("draft-1"), nav.current)
        see(Part2Strings.hrmsTitle).assertIsDisplayed()
        see(Part2Strings.hrmsRowHeading(1, 2)).assertIsDisplayed()
        see(Part2Strings.hrmsRowHeading(2, 2)).assertIsDisplayed()
        for (label in listOf(Part2Strings.hrmsCodeLabel, Part2Strings.hrmsNarrativeLabel, Part2Strings.fromLabel, Part2Strings.toLabel, Part2Strings.dataLabel(1), Part2Strings.dataLabel(2))) {
            assertTrue(exact(label), "label '$label'")
        }
        see("Granted the first thing for 62 days in 2026 from 05/07/2026 to 04/09/2026").assertIsDisplayed()
        composeRule.onNodeWithText("05/07/2026").assertIsDisplayed()
        composeRule.onNodeWithText("Certified that the second thing is in order").assertIsDisplayed()
        assertTrue(!exact(Part2Strings.authorityLabel), "the certificate is not under a label of ours")
        assertTrue(!exact(Part2Strings.hrmsNarrativeLabel + ":"), "a label is not folded into its value")
        back()
        assertEquals(Part2Destination.Slip("draft-1"), nav.current)
    }

    @Test
    fun thePreviewShowsEachRowsFromToAndDataLinesAsThePaperDoes() {
        openSlip()

        see(Part2Strings.fromToLine("05/07/2026", "04/09/2026")).assertIsDisplayed()
        see(Part2Strings.dataLine(1, "62")).assertIsDisplayed()
    }

    @Test
    fun theHrmsCheckListsTheRowsOfEveryOrderNotOnlyTheFirst() {
        val values = mapOf("leave_from" to SlotValue.DateValue(day(7, 5)), "leave_to" to SlotValue.DateValue(day(9, 4)), "year" to SlotValue.NumberValue(2026))
        val claim = OccurrenceRequest(SyntheticPart2Templates.CLAIM, mapOf("claim_on" to SlotValue.DateValue(day(8, 1))))
        val draft = Part2Draft("two-orders", LeaveEventInput(listOf(OccurrenceRequest(SyntheticPart2Templates.GRANT, values), claim)), emptyMap(), 1L, 2L)
        runBlocking { repository.saveDraft(draft) }

        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Hrms("two-orders"))))

        see(Part2Strings.hrmsRowHeading(1, 2)).assertIsDisplayed()
        see(Part2Strings.hrmsRowHeading(2, 2)).assertIsDisplayed()
        see("Claimed the third thing").assertIsDisplayed()
    }

    @Test
    fun aRestoredCheckLevelForADraftNotFinishedSaysSoInsteadOfShowingNothing() {
        val unfinished = Part2Draft("half", LeaveEventInput(listOf(OccurrenceRequest(SyntheticPart2Templates.GRANT))), emptyMap(), 1L, 2L)
        runBlocking { repository.saveDraft(unfinished) }

        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Hrms("half"))))

        see(Part2Strings.draftNotFinished).assertIsDisplayed()
    }

    @Test
    fun aRowFieldTheManualLeavesEmptyIsNotShownInTheHrmsCheck() {
        val nav = show()
        path()
        draftRow()
        fillLeave(model(nav))
        tap(Part2Strings.seeSlip)
        tap(Part2Strings.hrmsCheckButton)

        assertTrue(exact(Part2Strings.dataLabel(2)))
        assertTrue(!exact(Part2Strings.dataLabel(3)), "GRANT leaves Data 3 empty")
        assertTrue(!exact(Part2Strings.authorityLabel))
    }

    @Test
    fun aRestoredCheckLevelForSomeoneWithNoPremiumAndNoDraftShowsTheUpgradePanelAndNothingOfADraft() {
        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Hrms("old-1"))), unlocked = false, part2Unlocked = false)

        see(Part2Strings.lockedTitle).assertIsDisplayed()
        assertTrue(!has(Part2Strings.hrmsTitle))
    }
}
