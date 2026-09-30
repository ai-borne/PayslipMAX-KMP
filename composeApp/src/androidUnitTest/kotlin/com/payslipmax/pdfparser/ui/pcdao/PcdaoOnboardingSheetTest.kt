package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.onboarding.OnboardingStorage
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
@OptIn(ExperimentalTestApi::class)
class PcdaoOnboardingSheetTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private class TestOnboardingStorage(
        private var hasCompletedOnboarding: Boolean = false,
        private var hasSeenUploadCoachmark: Boolean = false,
        private var introSeen: Boolean = false,
    ) : OnboardingStorage {
        override fun getHasCompletedOnboarding(): Boolean = hasCompletedOnboarding

        override fun saveHasCompletedOnboarding(completed: Boolean) {
            hasCompletedOnboarding = completed
        }

        override fun getHasSeenUploadCoachmark(): Boolean = hasSeenUploadCoachmark

        override fun saveHasSeenUploadCoachmark(seen: Boolean) {
            hasSeenUploadCoachmark = seen
        }

        override fun getHasSeenPcdaoAuditIntro(): Boolean = introSeen

        override fun saveHasSeenPcdaoAuditIntro(seen: Boolean) {
            introSeen = seen
        }
    }

    @Test
    fun onboardingSheet_displaysFirstSlideInitially_withCorrectCopyAndCta() =
        runComposeUiTest {
            setContent {
                PcdaoOnboardingSheet(onDismiss = {})
            }

            onNodeWithTag("pcdao_onboarding_dialog").assertIsDisplayed()
            onNodeWithTag("pcdao_onboarding_slide_0").assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.onboardingSlide1Title).assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.onboardingSlide1Body).assertIsDisplayed()
            onNodeWithTag("pcdao_onboarding_skip").assertIsDisplayed()
            onNodeWithTag("pcdao_onboarding_next").assertIsDisplayed()
            onNodeWithTag("pcdao_onboarding_back").assertDoesNotExist()
        }

    @Test
    fun onboardingSheet_navigatesSlidesForwardAndBack() =
        runComposeUiTest {
            setContent {
                PcdaoOnboardingSheet(onDismiss = {})
            }

            // Step 1: Advance to Slide 2
            onNodeWithTag("pcdao_onboarding_next").performClick()
            waitForIdle()
            onNodeWithTag("pcdao_onboarding_slide_1").assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.onboardingSlide2Title).assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.onboardingSlide2Body).assertIsDisplayed()
            onNodeWithTag("pcdao_onboarding_back").assertIsDisplayed()
            onNodeWithTag("pcdao_onboarding_next").assertIsDisplayed()

            // Step 2: Advance to Slide 3
            onNodeWithTag("pcdao_onboarding_next").performClick()
            waitForIdle()
            onNodeWithTag("pcdao_onboarding_slide_2").assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.onboardingSlide3Title).assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.onboardingSlide3Body).assertIsDisplayed()
            onNodeWithTag("pcdao_onboarding_enter").assertIsDisplayed()

            // Step 3: Go back to Slide 2
            onNodeWithTag("pcdao_onboarding_back").performClick()
            waitForIdle()
            onNodeWithTag("pcdao_onboarding_slide_1").assertIsDisplayed()
        }

    @Test
    fun onboardingSheet_clickEnterCockpit_triggersDismiss() =
        runComposeUiTest {
            var dismissed = false
            setContent {
                PcdaoOnboardingSheet(onDismiss = { dismissed = true })
            }

            // Advance to slide 3
            onNodeWithTag("pcdao_onboarding_next").performClick()
            waitForIdle()
            onNodeWithTag("pcdao_onboarding_next").performClick()
            waitForIdle()

            // Click Enter Cockpit
            onNodeWithTag("pcdao_onboarding_enter").performClick()
            waitForIdle()
            assertTrue(dismissed)
        }

    @Test
    fun onboardingSheet_clickSkip_triggersDismiss() =
        runComposeUiTest {
            var dismissed = false
            setContent {
                PcdaoOnboardingSheet(onDismiss = { dismissed = true })
            }

            onNodeWithTag("pcdao_onboarding_skip").performClick()
            waitForIdle()
            assertTrue(dismissed)
        }

    @Test
    fun topNavBar_showsGuideButton_andTriggersCallback() =
        runComposeUiTest {
            var guideClicked = false
            setContent {
                TopNavBar(
                    onBack = {},
                    onGuideClick = { guideClicked = true },
                )
            }

            val guideButton = onNodeWithTag("pcdao_guide_button")
            guideButton.assertIsDisplayed()
            guideButton.performClick()
            waitForIdle()
            assertTrue(guideClicked)
        }

    @Test
    fun onboardingManager_persistsIntroDismissalState() {
        val storage = TestOnboardingStorage(introSeen = false)
        val manager = OnboardingManager(storage)

        assertTrue(manager.shouldShowPcdaoAuditIntro())
        assertFalse(storage.getHasSeenPcdaoAuditIntro())

        manager.onPcdaoAuditIntroDismissed()

        assertFalse(manager.shouldShowPcdaoAuditIntro())
        assertTrue(storage.getHasSeenPcdaoAuditIntro())
    }
}
