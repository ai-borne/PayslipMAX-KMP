package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideTabTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGuideRepository()
    private val viewModel = GuideViewModel(repository, FakeCrashReporter(), dispatcher)

    private fun show(navState: GuideNavState = GuideNavState()): GuideNavState {
        composeRule.setContent { GuideTab(navState = navState, viewModel = viewModel) }
        return navState
    }

    private fun finishLoading() {
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun pressBack() = composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun showsLoadingThenOneTilePerArea() {
        show()
        composeRule.onNodeWithContentDescription(GuideStrings.loading).assertIsDisplayed()

        finishLoading()

        composeRule.onNodeWithText(GuideStrings.homeTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Travel").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.topicCount(2)).assertIsDisplayed()
        composeRule.onNodeWithText("Pay").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.topicCount(1)).assertIsDisplayed()
    }

    @Test
    fun aFailedLoadShowsAnErrorWithRetryNeverABlankTab() {
        repository.result = GuideLoadResult.Failed(GuideLoadError.READ_FAILED)
        show()
        finishLoading()
        composeRule.onNodeWithText(GuideStrings.loadFailedTitle).assertIsDisplayed()

        repository.result = GuideBundleParser.parse(SyntheticGuideBundle.JSON)
        composeRule.onNodeWithText(GuideStrings.retry).performClick()
        finishLoading()

        composeRule.onNodeWithText("Travel").assertIsDisplayed()
    }

    @Test
    fun anAreaListsItsCasesWithRuleSubtitlesAndCardCounts() {
        show()
        finishLoading()
        composeRule.onNodeWithText("Travel").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Daily allowance on duty").assertIsDisplayed()
        composeRule.onNodeWithText("Rule 114").assertIsDisplayed()
        // The pill is read as "8 cards", not a bare number; 8 = cards homed in the case.
        composeRule.onNodeWithContentDescription(GuideStrings.cardCount(8)).assertIsDisplayed()
        // 3 = two homed cards plus one "also relevant here" link: what the feed will list.
        composeRule.onNodeWithContentDescription(GuideStrings.cardCount(3)).assertIsDisplayed()
    }

    @Test
    fun theOnScreenBackHeaderGoesUpOneLevel() {
        // iOS has no edge-swipe inside a tab, so this header is the only way up there.
        val nav = show()
        finishLoading()
        composeRule.onNodeWithText("Travel").performClick()
        composeRule.onNodeWithText("Home town LTC").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(GuideStrings.comingNext).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(AppStrings.btnBack).performClick()
        composeRule.waitForIdle()

        assertEquals(GuideDestination.Area("travel"), nav.current)
        composeRule.onNodeWithText("Home town LTC").assertIsDisplayed()
    }

    @Test
    fun androidBackPopsTheGuideStackBeforeLeaving() {
        val nav = show()
        finishLoading()
        composeRule.onNodeWithText("Travel").performClick()
        composeRule.waitForIdle()

        pressBack()
        composeRule.waitForIdle()
        assertEquals(GuideDestination.Home, nav.current)
        assertFalse(composeRule.activity.isFinishing, "back inside the Guide must not leave the app")

        pressBack()
        composeRule.waitForIdle()
        assertTrue(composeRule.activity.isFinishing, "at Guide Home back is not intercepted")
    }

    @Test
    fun aStackRestoredBeforeLoadingIsCutAtAnIdTheBundleNoLongerHas() {
        val restored = GuideNavStateSaver.restore(listOf("area|travel", "card|RB-GONE"))!!
        show(restored)
        finishLoading()

        assertEquals(GuideDestination.Area("travel"), restored.current)
        composeRule.onNodeWithText("Daily allowance on duty").assertIsDisplayed()
    }
}
