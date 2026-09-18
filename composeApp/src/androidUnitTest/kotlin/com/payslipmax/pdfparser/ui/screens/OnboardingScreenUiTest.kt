package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.payslipmax.pdfparser.ui.theme.AppStringsOnboarding
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingScreenUiTest {
    @Test
    fun allThreeSlidesReachableViaPagerSwipe() =
        runComposeUiTest {
            setContent {
                OnboardingScreen(onFinished = {}, onNavigateToFaq = {})
            }

            onNodeWithText(AppStringsOnboarding.onboardingSlide1Title).assertExists()

            onNodeWithTag("onboarding_pager").performTouchInput { swipeLeft() }
            waitForIdle()
            onNodeWithText(AppStringsOnboarding.onboardingSlide2Title).assertExists()

            onNodeWithTag("onboarding_pager").performTouchInput { swipeLeft() }
            waitForIdle()
            onNodeWithText(AppStringsOnboarding.onboardingSlide3Title).assertExists()
        }

    @Test
    fun skipOnFirstSlideTriggersOnFinished() =
        runComposeUiTest {
            var finishedCount = 0

            setContent {
                OnboardingScreen(onFinished = { finishedCount++ }, onNavigateToFaq = {})
            }

            onNodeWithTag("onboarding_skip").performClick()

            assertEquals(1, finishedCount)
        }

    @Test
    fun getStartedOnThirdSlideTriggersOnFinished() =
        runComposeUiTest {
            var finishedCount = 0

            setContent {
                OnboardingScreen(onFinished = { finishedCount++ }, onNavigateToFaq = {})
            }

            onNodeWithTag("onboarding_pager").performTouchInput { swipeLeft() }
            waitForIdle()
            onNodeWithTag("onboarding_pager").performTouchInput { swipeLeft() }
            waitForIdle()

            onNodeWithText(AppStringsOnboarding.onboardingCtaGetStarted).performClick()

            assertEquals(1, finishedCount)
        }

    @Test
    fun faqLinkOnThirdSlideTriggersOnNavigateToFaq() =
        runComposeUiTest {
            var faqNavCount = 0

            setContent {
                OnboardingScreen(onFinished = {}, onNavigateToFaq = { faqNavCount++ })
            }

            onNodeWithTag("onboarding_pager").performTouchInput { swipeLeft() }
            waitForIdle()
            onNodeWithTag("onboarding_pager").performTouchInput { swipeLeft() }
            waitForIdle()

            onNodeWithText(AppStringsOnboarding.onboardingFaqLinkText).performClick()

            assertEquals(1, faqNavCount)
        }
}
