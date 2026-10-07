package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Phase E4: the search icon on Home, typing, results, and the way back from a card. Synthetic bundle: the two cards
 * homed in "ltc-home" (rule line "Rule 177A") are RB-T9 and RB-T10. A tall screen keeps every row composed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideSearchScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val guide = GuideViewModel(FakeGuideRepository(), FakeCrashReporter(), dispatcher)
    private val search = GuideSearchViewModel(guide, dispatcher)

    private fun show(navState: GuideNavState = GuideNavState()): GuideNavState {
        composeRule.setContent { GuideTab(navState = navState, access = UnlockedGuideAccess, viewModel = guide, searchViewModel = search) }
        settle()
        return navState
    }

    private fun settle() {
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun openSearch() {
        composeRule.onNodeWithContentDescription(GuideStrings.searchOpen).performClick()
        settle()
    }

    private fun type(text: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(text)
        settle()
    }

    private fun pressBack() = composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

    private fun title(id: String) = "Synthetic card $id?"

    @Test
    fun theSearchIconOnHomeOpensSearchWithAHintAndNoResults() {
        val nav = show()

        openSearch()

        assertEquals(GuideDestination.Search, nav.current)
        composeRule.onNodeWithText(GuideStrings.searchHint).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.searchPlaceholder).assertIsDisplayed()
    }

    @Test
    fun aRuleNumberShowsItsCardsWithTheCaseTheyLiveInAndACount() {
        show()
        openSearch()

        type("177")

        composeRule.onNodeWithText(GuideStrings.resultCount(2)).assertIsDisplayed()
        composeRule.onNodeWithText(title("RB-T9")).assertIsDisplayed()
        composeRule.onNodeWithText(title("RB-T10")).assertIsDisplayed()
        composeRule.onNodeWithText(title("RB-T1")).assertDoesNotExist()
        composeRule.onAllNodesWithText("Home town LTC").assertCountEquals(2) // one pill on each result
    }

    @Test
    fun oneLetterShowsAHintAndAMissShowsNoCards() {
        show()
        openSearch()

        type("a")
        composeRule.onNodeWithText(GuideStrings.searchTooShort).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(GuideStrings.searchClear).performClick()
        settle()
        type("zebra")
        composeRule.onNodeWithText(GuideStrings.searchNone).assertIsDisplayed()
    }

    @Test
    fun openingAResultPushesTheCardAndBackReturnsToTheResultsWithTheQuery() {
        val nav = show()
        openSearch()
        type("177")

        composeRule.onNodeWithText(title("RB-T10")).performClick()
        settle()
        assertEquals(listOf(GuideDestination.Home, GuideDestination.Search, GuideDestination.Card("RB-T10")), nav.stack)
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(AppStrings.btnBack).performClick()
        settle()

        assertEquals(GuideDestination.Search, nav.current)
        composeRule.onNodeWithText("177").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.resultCount(2)).assertIsDisplayed()
    }

    @Test
    fun androidBackFromACardAndFromSearchWalksBackToHome() {
        val nav = show()
        openSearch()
        type("177")
        composeRule.onNodeWithText(title("RB-T9")).performClick()
        settle()

        pressBack()
        settle()
        assertEquals(GuideDestination.Search, nav.current)

        pressBack()
        settle()
        assertEquals(GuideDestination.Home, nav.current)
    }

    @Test
    fun eachVisitToSearchStartsEmpty() {
        show()
        openSearch()
        type("177")

        composeRule.onNodeWithContentDescription(AppStrings.btnBack).performClick()
        settle()
        openSearch()

        composeRule.onNodeWithText(GuideStrings.searchHint).assertIsDisplayed()
        composeRule.onNodeWithText(title("RB-T9")).assertDoesNotExist()
    }

    @Test
    fun aSearchRestoredAfterProcessDeathHasNoQuery() {
        // The saved stack holds "search" and nothing typed: the query was never written (owner decision 2026-10-07).
        show(GuideNavStateSaver.restore(listOf("search"))!!)

        composeRule.onNodeWithText(GuideStrings.searchHint).assertIsDisplayed()
    }

    @Test
    fun clearingTheFieldEmptiesTheResults() {
        show()
        openSearch()
        type("177")

        composeRule.onNodeWithContentDescription(GuideStrings.searchClear).performClick()
        settle()

        composeRule.onNodeWithText(GuideStrings.searchHint).assertIsDisplayed()
        composeRule.onNodeWithText(title("RB-T9")).assertDoesNotExist()
    }
}
