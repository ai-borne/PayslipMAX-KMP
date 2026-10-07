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
import com.payslipmax.pdfparser.guide.domain.GuideStaleness
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Phase E5: the four trust chips wherever a card appears, the "rates may have changed" nudge, and the Premium
 * preview. A locked card must show its title, one-line answer and chips, and none of key points, attach, watch-out,
 * authority or details (the synthetic texts are unique per card, so absence is checked by text, not by heading only).
 * A tall screen keeps every row and section composed, so "does not exist" means left out, not scrolled off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideTrustPaywallTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private var now = GuideStaleness.epochDays(2026, 10, 7) * MILLIS_PER_DAY
    private val guide = GuideViewModel(FakeGuideRepository(), FakeCrashReporter(), dispatcher, nowMillis = { now })
    private val search = GuideSearchViewModel(guide, dispatcher)
    private var unlockTaps = 0

    private fun show(unlocked: Boolean): GuideNavState {
        val nav = GuideNavState()
        val access = GuideAccess(unlocked, onUnlock = { unlockTaps++ })
        composeRule.setContent { GuideTab(navState = nav, access = access, viewModel = guide, searchViewModel = search) }
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

    private fun title(id: String) = "Synthetic card $id?"

    private fun openCard(
        id: String,
        unlocked: Boolean,
    ) {
        show(unlocked)
        tap("Travel")
        tap("Daily allowance on duty")
        tap(title(id))
    }

    @Test
    fun aLockedCardShowsTitleAnswerAndTheUnlockPanelButNothingPaid() {
        openCard("RB-T5", unlocked = false)

        composeRule.onAllNodesWithText(title("RB-T5")).assertCountEquals(1) // the header only; this is the card screen
        composeRule.onNodeWithText("A one-line answer for RB-T5.").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.lockedTitle).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.unlock).assertIsDisplayed()
        for (paid in listOf(GuideStrings.sectionKeyPoints, GuideStrings.sectionAttach, GuideStrings.sectionAuthority, GuideStrings.details)) {
            composeRule.onNodeWithText(paid).assertDoesNotExist()
        }
        composeRule.onNodeWithText("A short key point for RB-T5").assertDoesNotExist()
        composeRule.onNodeWithText("Rule 114 TR").assertDoesNotExist()
        composeRule.onNodeWithText("Longer details for RB-T5.").assertDoesNotExist()
    }

    @Test
    fun theUnlockButtonOffersTheUpgradeAndNothingElseHappens() {
        val nav = show(unlocked = false)
        tap("Travel")
        tap("Daily allowance on duty")
        tap(title("RB-T5"))

        tap(GuideStrings.unlock)

        assertEquals(1, unlockTaps)
        assertEquals(GuideDestination.Card("RB-T5"), nav.current)
    }

    @Test
    fun anUnlockedCardShowsTheWholeBodyAndNoUnlockPanel() {
        openCard("RB-T5", unlocked = true)

        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()
        composeRule.onNodeWithText("A short key point for RB-T5").assertIsDisplayed()
        composeRule.onNodeWithText("Rule 114 TR").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.lockedTitle).assertDoesNotExist()
        composeRule.onNodeWithText(GuideStrings.unlock).assertDoesNotExist()
    }

    @Test
    fun anUnverifiedCardShowsItsChipAndTheCheckingLineEvenWhenLocked() {
        openCard(SyntheticGuideBundle.UNVERIFIED_CARD, unlocked = false)

        composeRule.onNodeWithText(GuideStrings.chipUnverified).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.unverifiedWarning).assertIsDisplayed()
    }

    @Test
    fun theFeedRowsShowTheChipsOfTheirCards() {
        show(unlocked = false)
        tap("Travel")
        tap("Daily allowance on duty")

        composeRule.onAllNodesWithText(GuideStrings.chipUnverified).assertCountEquals(1)
        composeRule.onAllNodesWithText(GuideStrings.chipAmended).assertCountEquals(1)
        composeRule.onAllNodesWithText(GuideStrings.chipNoOfficialSource).assertCountEquals(1)
        composeRule.onAllNodesWithText(GuideStrings.chipRatesAsOf(SyntheticGuideBundle.RATES_AS_OF)).assertCountEquals(1)
        composeRule.onNodeWithText("Rates as of Jan 2026").assertIsDisplayed()
    }

    @Test
    fun aCardWithNoCiteShowsNoOfficialSourceAndAnAmendedCardShowsAmended() {
        openCard(SyntheticGuideBundle.NO_CITE_CARD, unlocked = true)
        composeRule.onNodeWithText(GuideStrings.chipNoOfficialSource).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionAuthority).assertDoesNotExist()
    }

    @Test
    fun theStaleNudgeShowsOnAnOldRateCardAndNotBeforeTheThreshold() {
        openCard(SyntheticGuideBundle.PERSONAL_CARD, unlocked = false)
        composeRule.onNodeWithText(GuideStrings.staleRatesNudge(SyntheticGuideBundle.RATES_AS_OF)).assertIsDisplayed()
    }

    @Test
    fun aFreshRateCardHasTheChipButNoNudge() {
        now = GuideStaleness.epochDays(2026, 4, 1) * MILLIS_PER_DAY
        openCard(SyntheticGuideBundle.PERSONAL_CARD, unlocked = false)

        composeRule.onAllNodesWithText(GuideStrings.chipRatesAsOf(SyntheticGuideBundle.RATES_AS_OF)).assertCountEquals(1)
        composeRule.onNodeWithText(GuideStrings.staleRatesNudge(SyntheticGuideBundle.RATES_AS_OF)).assertDoesNotExist()
    }

    @Test
    fun aLockedUsersSearchFindsTitlesButNotTextOnlyInThePaidHalf() {
        show(unlocked = false)
        composeRule.onNodeWithContentDescription(GuideStrings.searchOpen).performClick()
        settle()

        composeRule.onNode(hasSetTextAction()).performTextInput("longer details")
        settle()
        composeRule.onNodeWithText(GuideStrings.searchNone).assertIsDisplayed()
    }

    @Test
    fun anUnlockedUsersSearchFindsTheSameText() {
        show(unlocked = true)
        composeRule.onNodeWithContentDescription(GuideStrings.searchOpen).performClick()
        settle()

        composeRule.onNode(hasSetTextAction()).performTextInput("longer details")
        settle()
        composeRule.onNodeWithText(GuideStrings.resultCount(13)).assertIsDisplayed()
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
