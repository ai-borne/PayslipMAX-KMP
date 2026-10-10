package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * M4 on screen, on a synthetic bundle with one replaced pair and one change entry (no real rule is replaced yet): the Home
 * row, the list and its card links, the Updated chip in a feed, and the links between an old and a new rule. The plain
 * bundle must look exactly as before, and a locked user gets the links but never the paid half of either card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideRuleChangeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val storage = FakeGuidePinsStorage()
    private val oldTitle = "Synthetic card ${SyntheticGuideRuleChange.OLD_CARD}?"
    private val newTitle = "Synthetic card ${SyntheticGuideRuleChange.NEW_CARD}?"
    private val effectiveLabel = "15 Nov 2026"

    private fun show(
        unlocked: Boolean = true,
        plain: Boolean = false,
        nav: GuideNavState = GuideNavState(),
    ): GuideNavState {
        val repository = if (plain) FakeGuideRepository() else FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideRuleChange.bundle()))
        val guide = GuideViewModel(repository, FakeCrashReporter(), dispatcher, pins = GuidePinsModel(storage))
        val search = GuideSearchViewModel(guide, dispatcher)
        composeRule.setContent { GuideTab(navState = nav, access = GuideAccess(unlocked, onUnlock = {}), viewModel = guide, searchViewModel = search) }
        settle()
        return nav
    }

    private fun settle() {
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    private fun openFeed() {
        tap("Travel")
        tap("Home town LTC")
    }

    @Test
    fun homeShowsTheWhatsNewRowNamedForTheNewestMonthAndOpensTheList() {
        val nav = show()

        composeRule.onNodeWithText("What's new in the Guide (Nov 2026)").assertIsDisplayed()
        tap("What's new in the Guide (Nov 2026)")

        assertEquals(GuideDestination.Changes, nav.current)
        composeRule.onNodeWithText(effectiveLabel).assertIsDisplayed()
        composeRule.onNodeWithText(SyntheticGuideRuleChange.LATEST_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText(SyntheticGuideRuleChange.CLARIFIED_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText(SyntheticGuideRuleChange.OLDER_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText("1 Jun 2026").assertIsDisplayed()
    }

    @Test
    fun theHomeOfABundleWithNoChangesHasNoWhatsNewRowAtAll() {
        show(plain = true)

        composeRule.onNodeWithText(GuideStrings.homeTitle).assertIsDisplayed()
        composeRule.onAllNodesWithText("What's new", substring = true).assertCountEquals(0)
    }

    @Test
    fun aCardLinkOpensTheCardAndBackReturnsToTheList() {
        val nav = show()
        tap("What's new in the Guide (Nov 2026)")

        composeRule.onAllNodesWithText(newTitle)[0].performClick()
        settle()
        assertEquals(GuideDestination.Card(SyntheticGuideRuleChange.NEW_CARD), nav.current)
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()

        composeRule.runOnUiThread { nav.pop() }
        settle()
        assertEquals(GuideDestination.Changes, nav.current)
        composeRule.onNodeWithText(SyntheticGuideRuleChange.LATEST_TEXT).assertIsDisplayed()
    }

    @Test
    fun aTabReTapFromTheListGoesBackToGuideHome() {
        val nav = show()
        tap("What's new in the Guide (Nov 2026)")

        composeRule.runOnUiThread { nav.popToHome() }
        settle()

        assertEquals(GuideDestination.Home, nav.current)
        composeRule.onNodeWithText("What's new in the Guide (Nov 2026)").assertIsDisplayed()
    }

    @Test
    fun aRestoredListOpensAfterTheBundleLoads() {
        // Process death: the saver wrote "changes"; the restored stack is checked against the bundle once it has loaded.
        val restored = GuideNavState(listOf(GuideDestination.Changes))
        val nav = show(nav = restored)

        assertEquals(GuideDestination.Changes, nav.current)
        composeRule.onNodeWithText(SyntheticGuideRuleChange.LATEST_TEXT).assertIsDisplayed()
    }

    @Test
    fun theFeedMarksOnlyTheCardsOfTheNewestEntryAsUpdatedAndListsTheNewRuleNotTheOld() {
        show()
        openFeed()

        composeRule.onAllNodesWithText(GuideMaintenanceStrings.chipUpdated).assertCountEquals(2)
        composeRule.onNodeWithText(newTitle).assertIsDisplayed()
        composeRule.onNodeWithText(oldTitle).assertDoesNotExist()
    }

    @Test
    fun theNewRuleOffersTheEarlierRuleAndTheEarlierRuleOffersTheCurrentOne() {
        val nav = show()
        openFeed()
        tap(newTitle)

        composeRule.onNodeWithText(GuideMaintenanceStrings.chipUpdated).assertIsDisplayed()
        tap(GuideMaintenanceStrings.earlierRule(SyntheticGuideRuleChange.EFFECTIVE))
        assertEquals(GuideDestination.Card(SyntheticGuideRuleChange.OLD_CARD), nav.current)
        composeRule.onNodeWithText(GuideMaintenanceStrings.chipReplacedOn(SyntheticGuideRuleChange.EFFECTIVE)).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.replacedNotice).assertIsDisplayed()
        composeRule.onNodeWithText("A short key point for RB-T9").assertIsDisplayed()

        tap(GuideMaintenanceStrings.seeCurrentRule)
        assertEquals(GuideDestination.Card(SyntheticGuideRuleChange.NEW_CARD), nav.current)
        composeRule.onNodeWithText("A short key point for RB-T11").assertIsDisplayed()
    }

    @Test
    fun aCardWithoutAnyHistoryShowsNoLinks() {
        show()
        tap("Travel")
        tap("Daily allowance on duty")
        tap("Synthetic card RB-T5?")

        composeRule.onNodeWithText(GuideMaintenanceStrings.seeCurrentRule).assertDoesNotExist()
        composeRule.onAllNodesWithText("Earlier rule", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText(GuideMaintenanceStrings.chipUpdated).assertCountEquals(0)
    }

    @Test
    fun aLockedUserGetsTheLinksButNoPaidHalfOfEitherRule() {
        show(unlocked = false)
        tap("What's new in the Guide (Nov 2026)")
        composeRule.onAllNodesWithText(oldTitle)[0].performClick()
        settle()

        composeRule.onNodeWithText(GuideMaintenanceStrings.chipReplacedOn(SyntheticGuideRuleChange.EFFECTIVE)).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.lockedTitle).assertIsDisplayed()
        composeRule.onNodeWithText("A short key point for RB-T9").assertDoesNotExist()
        tap(GuideMaintenanceStrings.seeCurrentRule)
        composeRule.onNodeWithText("A short key point for RB-T11").assertDoesNotExist()
    }

    @Test
    fun aPinnedReplacedCardStaysOnHomeWithItsChipAndStillOpens() {
        storage.stored = SyntheticGuideRuleChange.OLD_CARD
        val nav = show()

        composeRule.onNodeWithText(GuideStrings.pinnedSection).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.chipReplacedOn(SyntheticGuideRuleChange.EFFECTIVE)).assertIsDisplayed()
        tap(oldTitle)

        assertEquals(GuideDestination.Card(SyntheticGuideRuleChange.OLD_CARD), nav.current)
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()
    }

    @Test
    fun searchNeverListsTheReplacedRuleButFindsTheNewOne() {
        show()
        composeRule.onNodeWithContentDescription(GuideStrings.searchOpen).performClick()
        settle()
        composeRule.onNode(hasSetTextAction()).performTextInput("synthetic")
        settle()

        // Scrolled to, not assumed on screen: a result card's height is styling, the listing is what matters.
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(newTitle))
        composeRule.onNodeWithText(newTitle).assertIsDisplayed()
        composeRule.onNodeWithText(oldTitle).assertDoesNotExist()
    }
}
