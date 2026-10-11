package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.preclaim.CtgCase
import com.payslipmax.pdfparser.guide.preclaim.PreClaimIssue
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimIssueStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Plan 22, P7: what the worksheet shows for permanent duty and for every move alike. The questions each move asks, the portal fields
 * with a Copy button on exactly the boxes that have a value (never copy-all), the documents, and the estimate rows that say
 * "Estimate, rule, as of". Numbers are the owner's claim A profile (level 13A, basic Rs 1,49,000, DA 46%).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class PreClaimWorksheetUiTest : PreClaimUiBase() {
    private val marital = "Marital status"

    private fun copyButtons() = composeRule.onAllNodes(hasDescriptionStartingWith("Copy "))

    @Test
    fun permanentDutyAsksFourQuestionsAndTemporaryDutyOnlyTwoAndLeaveTravelFour() {
        show()
        openFromHome("Permanent duty")
        composeRule.onAllNodes(isToggleable()).assertCountEquals(QUESTION_TICKS_PD + 1)
        composeRule.onAllNodes(maritalBox(marital)).assertCountEquals(1)
        tap(PreClaimStrings.changeMove)
        tap("Temporary duty")
        composeRule.onAllNodes(isToggleable()).assertCountEquals(1)
        composeRule.onAllNodes(maritalBox(marital)).assertCountEquals(1)
        tap(PreClaimStrings.changeMove)
        tap("Leave travel")
        composeRule.onAllNodes(isToggleable()).assertCountEquals(QUESTION_TICKS_PD)
        composeRule.onAllNodes(maritalBox(marital)).assertCountEquals(1)
    }

    @Test
    fun theFieldListHasACopyButtonOnlyOnTheBoxesTheAppCanFillAndNeverACopyAll() {
        show()
        openFromHome("Permanent duty")
        copyButtons().assertCountEquals(3)
        listOf("Basic pay", "NPA", "MSP").forEach { composeRule.onNodeWithContentDescription("Copy $it").assertIsDisplayed() }
        tapDescribed("Copy Basic pay")
        assertEquals(listOf("149000"), copied)
        composeRule.onNodeWithText(PreClaimStrings.copied).assertIsDisplayed()
        composeRule.onAllNodesWithText("Copy all", substring = true, ignoreCase = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Share", substring = true, ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun aCopyButtonNamesTheBoxButNeverHoldsTheValueSoNoNumberLeaksIntoTheSemanticsTree() {
        show()
        openFromHome("Permanent duty")
        val described = copyButtons().fetchSemanticsNodes().flatMap { node -> node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
        assertEquals(3, described.size)
        assertFalse(described.any { text -> text.any { it.isDigit() } }, "a content description carries a number: $described")
    }

    @Test
    fun theUnconfirmedFieldIsMarkedOnceAndOnlyTheFieldListSaysSo() {
        show()
        openFromHome("Permanent duty")
        composeRule.onAllNodesWithText(PreClaimStrings.unverified).assertCountEquals(1)
        composeRule.onAllNodesWithText(PreClaimStrings.sourcePortal).assertCountEquals(1)
    }

    @Test
    fun theDocumentChecklistTheUploadRuleAndTheMoveNotesAreShown() {
        show()
        openFromHome("Permanent duty")
        composeRule.onNodeWithText("${GuideStrings.bullet} The posting order").assertIsDisplayed()
        composeRule.onNodeWithText("Upload 1 PDF file, more than 50 KB and less than 2 MB.").assertIsDisplayed()
        composeRule.onNodeWithText("The portal sets the advance.").assertIsDisplayed()
    }

    @Test
    fun theTransferGrantRowSaysEstimateTheRuleAndTheDateAndNeverAddsATotal() {
        show()
        openFromHome("Permanent duty")
        composeRule.onNodeWithText("Composite transfer grant").assertIsDisplayed()
        composeRule.onNodeWithText("₹1,19,200").assertIsDisplayed()
        composeRule.onNodeWithText("80% of basic pay ₹1,49,000").assertIsDisplayed()
        composeRule.onNodeWithText("Estimate. Rule:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("As of 2017-07-01.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(PreClaimStrings.estimatesNote).assertIsDisplayed()
        composeRule.onAllNodesWithText("total", substring = true, ignoreCase = true).assertCountEquals(0)
    }

    @Test
    fun aRoadDistanceTurnsBaggageFromAGapIntoAnExactEstimate() {
        show()
        openFromHome("Permanent duty")
        composeRule.onNodeWithText(PreClaimIssueStrings.of(PreClaimIssue.KM_MISSING)).assertIsDisplayed()
        type(PreClaimStrings.roadKm, "2881")
        composeRule.onNodeWithText("Baggage by road").assertIsDisplayed()
        composeRule.onNodeWithText("₹1,44,050").assertIsDisplayed()
        composeRule.onNodeWithText("2881 km at ₹50 a km").assertIsDisplayed()
    }

    @Test
    fun editingBasicPayChangesTheGrantAndUsePayslipValuesBringsBackTheOriginal() {
        show()
        openFromHome("Permanent duty")
        type(PreClaimStrings.basicPay, "100000")
        composeRule.onNodeWithText("₹80,000").assertIsDisplayed()
        tap(PreClaimStrings.resetProfile)
        composeRule.onNodeWithText("₹1,19,200").assertIsDisplayed()
    }

    @Test
    fun theAndamanNicobarTickPreSelectsTheIslandCaseAndTheOfficerCanStillChangeIt() {
        show()
        openFromHome("Permanent duty")
        tap("Destination is Andaman Nicobar")
        composeRule.onNodeWithText("₹1,49,000").assertIsDisplayed()
        tap(PreClaimStrings.ctgCase(CtgCase.NORMAL))
        composeRule.onNodeWithText("₹1,19,200").assertIsDisplayed()
    }

    @Test
    fun withBothSpousesServingTheGrantIsANoteAndNotAFigure() {
        show()
        openFromHome("Permanent duty")
        tap("Spouse in Army")
        composeRule.onNodeWithText(PreClaimIssueStrings.of(PreClaimIssue.BOTH_SPOUSES_SERVING)).assertIsDisplayed()
        composeRule.onAllNodesWithText("₹1,19,200").assertCountEquals(0)
    }

    @Test
    fun withNoPayslipTheBoxesStartEmptyAndTheOfficerTypesEveryValue() {
        payslips.current.value = null
        show()
        openFromHome("Permanent duty")
        composeRule.onNodeWithText(PreClaimIssueStrings.of(PreClaimIssue.BASIC_MISSING)).assertIsDisplayed()
        copyButtons().assertCountEquals(0)
        type(PreClaimStrings.basicPay, "149000")
        composeRule.onNodeWithText("Composite transfer grant").assertIsDisplayed()
        copyButtons().assertCountEquals(1)
    }

    private companion object {
        const val QUESTION_TICKS_PD = 3
    }
}
