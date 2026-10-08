package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideProfileProvider
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
 * Phase E6 on screen: the "Your figure" block belongs to the paid half, so it shows only on an unlocked card that has a
 * figure and a profile, and a locked or figure-less card never even reads the payslips. The screen holds a tall window so
 * "does not exist" means left out, not scrolled off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideYourFigureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val profile = GuideProfile(2026, 6, level = "14", basicPay = 144200.0, daPercent = 60, tptaClass = null, hraAmount = null)
    private val provider = FakeGuideProfileProvider(profile)
    private val crash = FakeCrashReporter()
    private val guide =
        GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideBundle.withFigures())), crash, dispatcher, profiles = provider)
    private val search = GuideSearchViewModel(guide, dispatcher)

    private fun settle() {
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    private fun openCard(
        id: String,
        unlocked: Boolean,
    ): GuideNavState {
        val nav = GuideNavState()
        composeRule.setContent { GuideTab(nav, GuideAccess(unlocked, onUnlock = {}), guide, search) }
        settle()
        tap("Travel")
        tap("Daily allowance on duty")
        tap("Synthetic card $id?")
        return nav
    }

    @Test
    fun anUnlockedFigureCardShowsTheLineWithItsBasis() {
        openCard(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true)

        composeRule.onNodeWithText(GuideStrings.sectionYourFigure).assertIsDisplayed()
        composeRule.onNodeWithText("₹1,500 a day").assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.figureFootnote("Jun 2026", 60, "Jul 2017")).assertIsDisplayed()
        composeRule.onNodeWithText("Rs {food_rate}", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("{", substring = true).assertDoesNotExist()
    }

    @Test
    fun aLockedCardShowsNoFigureAndNeverReadsThePayslips() {
        openCard(SyntheticGuideBundle.PERSONAL_CARD, unlocked = false)

        composeRule.onNodeWithText(GuideStrings.sectionYourFigure).assertDoesNotExist()
        composeRule.onNodeWithText("₹1,500 a day").assertDoesNotExist()
        assertEquals(0, provider.subscriptions, "a locked card must not even subscribe to the payslips")
    }

    @Test
    fun aCardWithoutAFigureNeverReadsThePayslips() {
        openCard("RB-T5", unlocked = true)

        composeRule.onNodeWithText(GuideStrings.sectionKeyPoints).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionYourFigure).assertDoesNotExist()
        assertEquals(0, provider.subscriptions)
    }

    @Test
    fun withoutAProfileTheCardIsCompleteWithoutTheBlock() {
        provider.current.value = null

        openCard(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true)

        composeRule.onNodeWithText(GuideStrings.sectionYourFigure).assertDoesNotExist()
        composeRule.onNodeWithText(GuideStrings.sectionAttach).assertIsDisplayed()
        composeRule.onNodeWithText(GuideStrings.sectionAuthority).assertIsDisplayed()
    }

    @Test
    fun theLineFollowsAnImportedPayslipAndStopsReadingWhenTheCardLeavesTheScreen() {
        val nav = openCard(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true)
        assertEquals(1, provider.active)

        provider.current.value = profile.copy(daPercent = 40)
        settle()
        composeRule.onNodeWithText("₹1,200 a day").assertIsDisplayed()

        composeRule.runOnIdle { nav.pop() }
        settle()
        assertEquals(0, provider.active, "reading stops with the screen, so no profile is held in memory")
    }

    @Test
    fun resolvingTheLineReportsNothingToTelemetry() {
        openCard(SyntheticGuideBundle.PERSONAL_CARD, unlocked = true)

        assertEquals(emptyList(), crash.exceptions)
        assertEquals(emptyList(), crash.logs)
        assertEquals(emptyMap(), crash.keys)
    }
}
