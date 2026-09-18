package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.onboarding.OnboardingStorage
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.theme.AppStringsOnboarding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardScreenCoachmarkUiTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val repository = PayslipRepository(FakePayslipDao(), FakePdfParser(), testDispatcher)
        viewModel = PayslipViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testUploadCoachmarkShowsOnceThenDismisses() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()

            setContent {
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdf = {},
                    onboardingManager = OnboardingManager(FakeOnboardingStorage(hasSeenUploadCoachmark = false)),
                )
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithTag("upload_coachmark").assertExists()
            onNodeWithText(AppStringsOnboarding.onboardingCoachmarkDismiss).performClick()
            onNodeWithTag("upload_coachmark").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testUploadCoachmarkNeverRendersWhenAlreadySeen() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()

            setContent {
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdf = {},
                    onboardingManager = OnboardingManager(FakeOnboardingStorage(hasSeenUploadCoachmark = true)),
                )
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithTag("upload_coachmark").assertDoesNotExist()
        }

    private class FakeOnboardingStorage(
        private var hasCompletedOnboarding: Boolean = true,
        private var hasSeenUploadCoachmark: Boolean = false,
    ) : OnboardingStorage {
        override fun getHasCompletedOnboarding(): Boolean = hasCompletedOnboarding

        override fun saveHasCompletedOnboarding(completed: Boolean) {
            hasCompletedOnboarding = completed
        }

        override fun getHasSeenUploadCoachmark(): Boolean = hasSeenUploadCoachmark

        override fun saveHasSeenUploadCoachmark(seen: Boolean) {
            hasSeenUploadCoachmark = seen
        }
    }
}
