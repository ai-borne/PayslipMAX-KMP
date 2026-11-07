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
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
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
        see(LearnStrings.sectionSubtitle).assertIsDisplayed()

        fun top(text: String) = composeRule.onAllNodesWithText(text)[0].fetchSemanticsNode().boundsInRoot.top
        val lastArea = (guide.uiState.value as GuideUiState.Ready).areas.map { GuideStrings.areaTileLabel(it.id) ?: it.title }.maxOf { top(it) }
        assertTrue(top(LearnStrings.sectionHeading) > lastArea, "the heading is below the last handbook area")
        assertTrue(top(LearnStrings.hubTitle) > top(LearnStrings.sectionHeading), "the tile is under the heading")
    }

    @Test
    fun guideHomeIsAGridWithOneWidePartTwoTileSoPartTwoIsNotBuriedUnderALongListOfRows() {
        // WHY: twelve one-per-row ribbons pushed Part II below the fold; two area tiles per row and ONE Part II tile keeps it close.
        // Learn and the Bank live one tap down, on the hub, never on Home.
        show()

        fun bounds(text: String) = composeRule.onAllNodesWithText(text)[0].fetchSemanticsNode().boundsInRoot
        val areas = (guide.uiState.value as GuideUiState.Ready).areas.map { bounds(GuideStrings.areaTileLabel(it.id) ?: it.title) }
        assertTrue(areas.size >= 2 && areas[0].top == areas[1].top && areas[0].left < areas[1].left, "the first two areas share a row")
        composeRule.onAllNodesWithText(LearnStrings.entryTitle).assertCountEquals(0)
        composeRule.onAllNodesWithText(OrdersBankStrings.entryTitle).assertCountEquals(0)
        assertTrue(composeRule.onAllNodesWithText(LearnStrings.hubTitle)[0].fetchSemanticsNode().boundsInRoot.left <= areas[0].left, "the Part II tile starts at the left edge")
    }

    @Test
    fun thePartTwoTileOpensAHubWithLearnAndTheBankAndBackReturnsToGuideHome() {
        val nav = show()
        tap(LearnStrings.hubTitle)
        assertEquals(Part2Destination.Hub, nav.current)
        see(LearnStrings.entryTitle).assertIsDisplayed()
        see(OrdersBankStrings.entryTitle).assertIsDisplayed()
        tap(LearnStrings.entryTitle)
        assertEquals(Part2Destination.Home, nav.current)
        nav.pop()
        assertEquals(Part2Destination.Hub, nav.current)
        nav.pop()
        assertEquals(com.payslipmax.pdfparser.ui.screens.guide.GuideDestination.Home, nav.current)
    }

    @Test
    fun withTheFlagOffTheHeadingIsAbsentToo() {
        show(part2 = null)
        composeRule.onAllNodesWithText(LearnStrings.sectionHeading).assertCountEquals(0)
    }

    @Test
    fun withTheFlagOnTheRowIsThereForAFreeUserAndOpensTheHomeWithEveryRibbon() {
        show(unlocked = false)
        openLearn()
        composeRule.onNodeWithText("The first ribbon").assertIsDisplayed()
        composeRule.onNodeWithText("The second ribbon").assertIsDisplayed()
        assertEquals(0, unlockTaps, "Learn is free: no upgrade prompt anywhere")
    }

    @Test
    fun theWholePathDownToACasualtyPageAndBackUpExactlyOneLevelAtATime() {
        val nav = show()
        openLearn()
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
        assertEquals(Part2Destination.Hub, nav.current)
        back()
        assertEquals(GuideDestination.Home, nav.current)
    }

    @Test
    fun aRibbonIsOneFlatListWithTheCaseNamesAsHeadingsAndNoCaseLevel() {
        val nav = show()
        openLearn()
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
        openLearn()
        tap("The second ribbon")
        tap("Granted a variant thing")
        assertEquals(Part2Destination.Code("VARIANT1"), nav.current, "a row with no search behind it opens on the first code")
        see("VARIANT2").assertIsDisplayed()
    }

    @Test
    fun searchFindsByCodeBySerialAndByNameAndOpensThePage() {
        val nav = show()
        openLearn()
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
        openLearn()
        type("variant2")
        tap("Granted a variant thing")
        assertEquals(Part2Destination.Code("VARIANT2"), nav.current)
        see("VARIANT1").assertIsDisplayed()
    }

    @Test
    fun backFromAPageKeepsTheQueryAndEachVisitFromGuideHomeStartsEmpty() {
        val nav = show()
        openLearn()
        type("fourth")
        tap("Granted the fourth thing")
        back()
        composeRule.onNodeWithText("Granted the fourth thing").assertIsDisplayed()
        assertEquals("fourth", model.query.value, "coming back from a page keeps the search")
        nav.popToHome()
        settle()
        openLearn()
        assertEquals("", model.query.value)
        see(LearnStrings.searchHint).assertIsDisplayed()
    }

    @Test
    fun theSearchBoxDoesNotTakeFocusSoTheKeyboardStaysDownUntilTheOfficerTapsIt() {
        show()
        openLearn()
        composeRule.onNode(hasSetTextAction()).assertIsNotFocused()
        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun searchSaysWhatToDoForNothingTooLittleAndNoMatch() {
        show()
        openLearn()
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
        openLearn()
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
