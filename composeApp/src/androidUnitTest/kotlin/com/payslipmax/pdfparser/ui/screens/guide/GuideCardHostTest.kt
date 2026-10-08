package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
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
 * E7: the card a Pay Audit finding opens, as `Screen.GuideCard` hosts it. Back is the host's Back (so it returns to the
 * finding); a restore with no target, an unknown card or a failed load pops instead of leaving a blank screen; a locked
 * user sees the free half and the unlock panel, never key points, cite or details.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideCardHostTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGuideRepository()
    private val crashReporter = FakeCrashReporter()
    private val guide = GuideViewModel(repository, crashReporter, dispatcher)
    private var backs = 0
    private var unlocks = 0

    private fun show(unlocked: Boolean = true) {
        composeRule.setContent { GuideCardHost(GuideAccess(unlocked, onUnlock = { unlocks++ }), onBack = { backs++ }, viewModel = guide) }
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    @Test
    fun aRestoredCardWithNoPendingTargetPopsItselfAndDrawsNothing() {
        show()

        assertEquals(1, backs, "process death lost the target: pop, never a blank screen")
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertDoesNotExist()
        assertEquals(0, repository.loadCount, "a restore must not load the Guide on its own")
    }

    @Test
    fun theOpenedCardIsShownAndTheBackHeaderReturnsToTheFinding() {
        guide.openCard("RB-T5")
        show()

        composeRule.onNodeWithText("Synthetic card RB-T5?").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()
        assertEquals(0, backs)
        composeRule.onNodeWithContentDescription(AppStrings.btnBack).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun aCardTheBundleDoesNotHoldPopsInsteadOfShowingABlankScreen() {
        guide.openCard("RB-GONE")
        show()

        assertEquals(1, backs)
    }

    @Test
    fun aBundleThatWillNotLoadPopsAndReportsOnlyTheLoadCode() {
        repository.result = GuideLoadResult.Failed(GuideLoadError.READ_FAILED)
        guide.openCard("RB-T5")
        show()

        assertEquals(1, backs)
        assertEquals(listOf(mapOf(GUIDE_LOAD_ERROR_KEY to "READ_FAILED")), crashReporter.exceptions.map { it.second })
    }

    @Test
    fun aLockedUserSeesTheFreeHalfAndTheUnlockPanelNeverTheRuleText() {
        guide.openCard("RB-T5")
        show(unlocked = false)

        composeRule.onNodeWithText("Synthetic card RB-T5?").assertIsDisplayed()
        composeRule.onNodeWithText("A short key point for RB-T5").assertDoesNotExist()
        composeRule.onNodeWithText("Rule 114 TR").assertDoesNotExist()
        composeRule.onNodeWithText(GuideStrings.unlock).performClick()
        assertEquals(1, unlocks)
        assertEquals(0, backs)
    }

    @Test
    fun noCardIdOrTextReachesTelemetryWhenACardOpens() {
        guide.openCard("RB-T5")
        show()

        assertEquals(emptyList(), crashReporter.exceptions)
        assertEquals(emptyMap(), crashReporter.keys)
        assertEquals(emptyList(), crashReporter.logs)
    }
}
