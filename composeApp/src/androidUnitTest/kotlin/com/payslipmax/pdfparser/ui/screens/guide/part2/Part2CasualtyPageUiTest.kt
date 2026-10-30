package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the casualty page is what the officer reads on the phone before he notes an event. It must say what the manual says (name, group
 * and its meaning, chapter, page), what the order needs in each column with the manual's own caption, the plain-English page where one was
 * written, and which casualties are usually published with it, labelled by whether the manual or practice says so. Where there is no data
 * there is no heading, and the one action it offers (Add to Orders Bank) is off until the Bank exists.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2CasualtyPageUiTest : Part2UiBase() {
    private var captionless = false

    override fun bundle() = SyntheticGuidePart2.bundle().let { if (captionless) it.copy(part2 = it.part2!!.copy(columns = SyntheticGuidePart2.bareColumns())) else it }

    private fun open(code: String): GuideNavState = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Code(code))))

    @Test
    fun thePageSaysWhatTheManualSaysAboutTheCasualty() {
        open("FIRSTCODE")
        see("Granted the first thing").assertIsDisplayed()
        see(LearnStrings.groupLabel("A")).assertIsDisplayed()
        see("Money").assertIsDisplayed()
        see(LearnStrings.chapterAndPage(1, 28)).assertIsDisplayed()
        see("1.1").assertIsDisplayed()
        count(LearnStrings.groupNotPrinted).assertCountEquals(0)
    }

    @Test
    fun aCasualtyTheManualGivesNoGroupSaysSoAndInfersNone() {
        open("STOPCODE")
        see(LearnStrings.groupNotPrinted).assertIsDisplayed()
    }

    @Test
    fun theColumnsListEachOneTheOrderUsesWithItsCaptionAndWhetherItIsRequired() {
        open("FIRSTCODE")
        see(LearnStrings.columnsHeading).assertIsDisplayed()
        see(LearnStrings.columnLabel(0)).assertIsDisplayed()
        see("dd/mm/yyyy").assertIsDisplayed()
        see(LearnStrings.columnLabel(2)).assertIsDisplayed()
        see("prefix").assertIsDisplayed()
        see(LearnStrings.columnLabel(3)).assertIsDisplayed()
        see("name").assertIsDisplayed()
        count(LearnStrings.needRequired).assertCountEquals(2)
        count(LearnStrings.needOptional).assertCountEquals(1)
        count(LearnStrings.columnLabel(1)).assertCountEquals(0)
        count(LearnStrings.columnLabel(4)).assertCountEquals(0)
    }

    @Test
    fun aRequiredColumnWithNoPrintedCaptionPointsToTheManualsPage() {
        captionless = true
        open("FIRSTCODE")
        see(LearnStrings.requiredNoCaption(28)).assertIsDisplayed()
    }

    @Test
    fun thePlainEnglishPageShowsItsFourPartsOnlyWhereOneWasWritten() {
        open("FIRSTCODE")
        see(LearnStrings.aboutHeading).assertIsDisplayed()
        for (part in listOf("What it is.", "When you see it.", "What to keep ready.", "One thing to watch.")) see(part).assertIsDisplayed()
        for (label in listOf(LearnStrings.aboutWhat, LearnStrings.aboutWhen, LearnStrings.aboutReady, LearnStrings.aboutWatch)) composeRule.onNodeWithText(label).assertIsDisplayed()
    }

    @Test
    fun aCasualtyWithNoPlainEnglishPageAndNoCompanionsShowsNeitherHeading() {
        open("SECONDCODE")
        see("Reported for the second thing").assertIsDisplayed()
        count(LearnStrings.aboutHeading).assertCountEquals(0)
        count(LearnStrings.relatedHeading).assertCountEquals(0)
    }

    @Test
    fun usuallyPublishedWithShowsEachCompanionWithItsManualPageOrThePracticeLabel() {
        open("FIRSTCODE")
        see(LearnStrings.relatedHeading).assertIsDisplayed()
        see("Reported for the second thing").assertIsDisplayed()
        see(LearnStrings.relatedBasis(29)).assertIsDisplayed()
        see("Ceased to draw the variant thing").assertIsDisplayed()
        see(LearnStrings.relatedPractice).assertIsDisplayed()
    }

    @Test
    fun tappingACompanionOpensItsPageAndBackReturns() {
        val nav = open("FIRSTCODE")
        tap("Reported for the second thing")
        assertEquals(Part2Destination.Code("SECONDCODE"), nav.current)
        back()
        assertEquals(Part2Destination.Code("FIRSTCODE"), nav.current)
    }

    @Test
    fun addToOrdersBankIsShownButOffUntilTheBankExists() {
        val nav = open("FIRSTCODE")
        composeRule.onNodeWithText(LearnStrings.addToBank).assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithText(LearnStrings.addToBank).performClick()
        settle()
        assertEquals(Part2Destination.Code("FIRSTCODE"), nav.current, "a disabled action goes nowhere")
    }
}
