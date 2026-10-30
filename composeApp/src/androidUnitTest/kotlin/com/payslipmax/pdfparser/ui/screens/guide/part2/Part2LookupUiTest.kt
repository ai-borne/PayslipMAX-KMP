package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavStateSaver
import com.payslipmax.pdfparser.ui.screens.guide.GuideUiState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the officer's path is Guide Home, then Learn, a ribbon, a casualty page, and also search straight to a casualty page; back must
 * undo exactly one level on both platforms, and the whole thing must be absent while the dark-launch flag is off. The data is the
 * invented section, so no manual wording is asserted here (the real bundle has its own test).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2LookupUiTest : Part2UiBase() {
    @Test
    fun withTheFlagOffNoLearnRowExistsAndKoinIsNeverAsked() {
        show(part2 = null)
        composeRule.onAllNodesWithText(LearnStrings.entryTitle).assertCountEquals(0)
        assertEquals(0, models.size)
    }

    @Test
    fun theLearnRowSitsUnderItsOwnHeadingBelowEveryHandbookAreaBecauseItsSourceIsADifferentManual() {
        show()
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.onNodeWithText(LearnStrings.sectionHeading).assert(heading).assertIsDisplayed()
        see(LearnStrings.sectionSubtext).assertIsDisplayed()

        fun top(text: String) = composeRule.onAllNodesWithText(text)[0].fetchSemanticsNode().boundsInRoot.top
        val lastArea = (guide.uiState.value as GuideUiState.Ready).areas.map { it.title }.maxOf { top(it) }
        assertTrue(top(LearnStrings.sectionHeading) > lastArea, "the heading is below the last handbook area")
        assertTrue(top(LearnStrings.entryTitle) > top(LearnStrings.sectionHeading), "the row is under the heading")
    }

    @Test
    fun withTheFlagOffTheHeadingIsAbsentToo() {
        show(part2 = null)
        composeRule.onAllNodesWithText(LearnStrings.sectionHeading).assertCountEquals(0)
    }

    @Test
    fun withTheFlagOnTheRowIsThereForAFreeUserAndOpensTheHomeWithEveryRibbon() {
        show(unlocked = false)
        tap(LearnStrings.entryTitle)
        composeRule.onNodeWithText("The first ribbon").assertIsDisplayed()
        composeRule.onNodeWithText("The second ribbon").assertIsDisplayed()
        assertEquals(0, unlockTaps, "Learn is free: no upgrade prompt anywhere")
    }

    @Test
    fun theWholePathDownToACasualtyPageAndBackUpExactlyOneLevelAtATime() {
        val nav = show()
        tap(LearnStrings.entryTitle)
        tap("The first ribbon")
        assertEquals(Part2Destination.Ribbon("leave"), nav.current)
        tap("Granted the fourth thing")
        assertEquals(Part2Destination.Code("FOURTH11"), nav.current)
        see(LearnStrings.chapterAndPage(3, 46)).assertIsDisplayed()
        back()
        assertEquals(Part2Destination.Ribbon("leave"), nav.current)
        back()
        assertEquals(Part2Destination.Home, nav.current)
        back()
        assertEquals(GuideDestination.Home, nav.current)
    }

    @Test
    fun aRibbonIsOneFlatListWithTheCaseNamesAsHeadingsAndNoCaseLevel() {
        val nav = show()
        tap(LearnStrings.entryTitle)
        tap("The first ribbon")
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.onNodeWithText("First case").assert(heading).assertIsDisplayed()
        composeRule.onNodeWithText("Second case").assert(heading).assertIsDisplayed()
        composeRule.onNodeWithText("Granted the first thing").assertIsDisplayed()
        composeRule.onNodeWithText("Released from the third thing").assertIsDisplayed()
        tap("First case")
        assertEquals(Part2Destination.Ribbon("leave"), nav.current, "a heading is not a place to go")
    }

    @Test
    fun aRibbonRowForAVariantSerialOpensItsPageOnTheFirstCodeAndShowsAllOfThem() {
        val nav = show()
        tap(LearnStrings.entryTitle)
        tap("The second ribbon")
        tap("Granted a variant thing")
        assertEquals(Part2Destination.Code("VARIANT1"), nav.current, "a row with no search behind it opens on the first code")
        see("VARIANT2").assertIsDisplayed()
    }

    @Test
    fun searchFindsByCodeBySerialAndByNameAndOpensThePage() {
        val nav = show()
        tap(LearnStrings.entryTitle)
        type("firstcode")
        composeRule.onNodeWithText("Granted the first thing").assertIsDisplayed()
        type("x") // "firstcodex" matches nothing
        see(LearnStrings.searchNone).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("3.28")
        settle()
        composeRule.onNodeWithText("Ceased to draw the variant thing").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("fourth")
        settle()
        tap("Granted the fourth thing")
        assertEquals(Part2Destination.Code("FOURTH11"), nav.current)
    }

    @Test
    fun aVariantSerialsPageShowsAllItsCodesWhicheverOneWasSearched() {
        val nav = show()
        tap(LearnStrings.entryTitle)
        type("variant2")
        tap("Granted a variant thing")
        assertEquals(Part2Destination.Code("VARIANT2"), nav.current)
        see("VARIANT1").assertIsDisplayed()
    }

    @Test
    fun backFromAPageKeepsTheQueryAndEachVisitFromGuideHomeStartsEmpty() {
        val nav = show()
        tap(LearnStrings.entryTitle)
        type("fourth")
        tap("Granted the fourth thing")
        back()
        composeRule.onNodeWithText("Granted the fourth thing").assertIsDisplayed()
        assertEquals("fourth", model.query.value, "coming back from a page keeps the search")
        nav.popToHome()
        settle()
        tap(LearnStrings.entryTitle)
        assertEquals("", model.query.value)
        see(LearnStrings.searchHint).assertIsDisplayed()
    }

    @Test
    fun theSearchBoxDoesNotTakeFocusSoTheKeyboardStaysDownUntilTheOfficerTapsIt() {
        show()
        tap(LearnStrings.entryTitle)
        composeRule.onNode(hasSetTextAction()).assertIsNotFocused()
        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun searchSaysWhatToDoForNothingTooLittleAndNoMatch() {
        show()
        tap(LearnStrings.entryTitle)
        see(LearnStrings.searchHint).assertIsDisplayed()
        type("a")
        see(LearnStrings.searchTooShort).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("zzzzqq")
        settle()
        see(LearnStrings.searchNone).assertIsDisplayed()
    }

    @Test
    fun aLockDropsTheTypedSearchAndTheOfficerComesBackToTheSameLevel() {
        val nav = show()
        tap(LearnStrings.entryTitle)
        type("fourth")
        nav.discardTransientForms() // what App does when the app locks
        settle()
        assertEquals("", model.query.value)
        see(LearnStrings.searchHint).assertIsDisplayed()
        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun aRestoredStackReopensTheSameCasualtyPageFromIdsAlone() {
        val before = GuideNavState(listOf(Part2Destination.Home, Part2Destination.Ribbon("leave"), Part2Destination.Code("FOURTH11")))
        val saved = with(GuideNavStateSaver) { SaverScope { true }.save(before) } as List<*>
        val nav = show(GuideNavStateSaver.restore(saved)!!)
        see(LearnStrings.chapterAndPage(3, 46)).assertIsDisplayed()
        assertEquals(Part2Destination.Code("FOURTH11"), nav.current)
    }

    @Test
    fun aRestoredPlaceTheBundleNoLongerHasIsCutBackToTheLastKnownLevel() {
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Ribbon("nope"))))
        assertEquals(Part2Destination.Home, nav.current)
        see(LearnStrings.searchHint).assertIsDisplayed()
    }

    @Test
    fun withTheFlagOffAfterARestoreNoPartTwoLevelIsLeftAndGuideHomeShows() {
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Code("FOURTH11"))), part2 = null)
        assertEquals(GuideDestination.Home, nav.current)
        composeRule.onAllNodesWithText(LearnStrings.entryTitle).assertCountEquals(0)
    }
}
