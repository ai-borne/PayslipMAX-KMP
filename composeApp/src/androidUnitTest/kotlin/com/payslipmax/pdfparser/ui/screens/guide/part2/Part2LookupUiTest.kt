package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the officer's path is Guide Home, then Part II, a ribbon, a case, a code page, and also search straight to a code page; back
 * must undo each step on both platforms, and the whole thing must be absent while the dark-launch flag is off. The data is the
 * invented lookup, so no manual wording is asserted here (the real bundle has its own test).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2LookupUiTest : Part2UiBase() {
    @Test
    fun withTheFlagOffNoPartTwoRowExistsAndKoinIsNeverAsked() {
        show(part2 = null)
        composeRule.onAllNodesWithText(Part2Strings.entryTitle).assertCountEquals(0)
        assertEquals(0, models.size)
    }

    @Test
    fun withTheFlagOnTheRowIsThereForAFreeUserAndOpensTheHomeWithBuiltRibbonsOnly() {
        show(unlocked = false)
        tap(Part2Strings.entryTitle)
        composeRule.onNodeWithText("The first ribbon").assertIsDisplayed()
        composeRule.onNodeWithText("The second ribbon").assertIsDisplayed()
        composeRule.onAllNodesWithText("The third ribbon").assertCountEquals(0)
        assertEquals(0, unlockTaps, "the lookup is free: no upgrade prompt anywhere")
        assertEquals(1, models.size)
    }

    @Test
    fun theWholePathDownToACodePageAndBackUpEveryLevel() {
        val nav = show()
        tap(Part2Strings.entryTitle)
        tap("The first ribbon")
        composeRule.onNodeWithText("First case").assertIsDisplayed()
        tap("Second case")
        composeRule.onNodeWithText("Granted the fourth thing").assertIsDisplayed()
        tap("Rejoined from the invented leave")
        assertEquals(Part2Destination.Code("REJOINAL"), nav.current)
        see(Part2Strings.chapterAndPage(5, 131)).assertIsDisplayed()
        back()
        assertEquals(Part2Destination.Case("leave_two"), nav.current)
        back()
        assertEquals(Part2Destination.Ribbon("leave"), nav.current)
        back()
        assertEquals(Part2Destination.Home, nav.current)
        back()
        assertEquals(GuideDestination.Home, nav.current)
    }

    @Test
    fun aCaseListsLookupOnlyCodesMarkedAndAlsoCodesUnderTheirOwnHeading() {
        show()
        tap(Part2Strings.entryTitle)
        tap("The first ribbon")
        tap("First case")
        see(Part2Strings.alsoSeeHeading).assertIsDisplayed()
        see("Granted the fourth thing").assertIsDisplayed()
        back()
        tap("Second case")
        count(Part2Strings.lookupOnly).assertCountEquals(1)
    }

    @Test
    fun theCodePageForASerialWithNoPrintedGroupSaysSoAndInfersNone() {
        show()
        tap(Part2Strings.entryTitle)
        tap("The second ribbon")
        tap("Third case")
        tap("Ceased to draw the variant thing")
        see(Part2Strings.groupNotPrinted).assertIsDisplayed()
    }

    @Test
    fun aCodePageShowsTheGroupAndWhatItMeans() {
        show()
        tap(Part2Strings.entryTitle)
        tap("The first ribbon")
        tap("First case")
        tap("Granted the first thing")
        see(Part2Strings.groupLabel("A")).assertIsDisplayed()
        see("Money").assertIsDisplayed()
        see(Part2Strings.chapterAndPage(1, 28)).assertIsDisplayed()
        count(Part2Strings.groupNotPrinted).assertCountEquals(0)
    }

    @Test
    fun aVariantSerialsPageShowsAllItsCodes() {
        val nav = show()
        tap(Part2Strings.entryTitle)
        type("variant2")
        tap("Granted a variant thing")
        assertEquals(Part2Destination.Code("VARIANT2"), nav.current)
        see("VARIANT1").assertIsDisplayed()
        see("VARIANT2").assertIsDisplayed()
    }

    @Test
    fun rejoinalsPageSaysTheSlipPrintsRejoin() {
        show()
        tap(Part2Strings.entryTitle)
        type("rejoinal")
        tap("Rejoined from the invented leave")
        see(Part2Strings.printedAsNote("REJOINAL", "REJOIN")).assertIsDisplayed()
    }

    @Test
    fun searchReachesAnUnbuiltRibbonsCodeAndBackKeepsTheQuery() {
        show()
        tap(Part2Strings.entryTitle)
        type("later")
        composeRule.onNodeWithText("Granted a later thing").assertIsDisplayed()
        tap("Granted a later thing")
        see(Part2Strings.chapterAndPage(4, 109)).assertIsDisplayed()
        back()
        composeRule.onNodeWithText("Granted a later thing").assertIsDisplayed()
        assertEquals("later", models.single().query.value, "coming back from a code page keeps the search")
    }

    @Test
    fun searchSaysWhatToDoForNothingTooLittleAndNoMatch() {
        show()
        tap(Part2Strings.entryTitle)
        see(Part2Strings.searchHint).assertIsDisplayed()
        type("a")
        see(Part2Strings.searchTooShort).assertIsDisplayed()
        type("zzzzqq")
        see(Part2Strings.searchNone).assertIsDisplayed()
    }

    @Test
    fun eachVisitFromGuideHomeStartsWithAnEmptySearch() {
        val nav = show()
        tap(Part2Strings.entryTitle)
        type("later")
        nav.popToHome()
        settle()
        tap(Part2Strings.entryTitle)
        assertEquals("", models.single().query.value)
        see(Part2Strings.searchHint).assertIsDisplayed()
    }
}
