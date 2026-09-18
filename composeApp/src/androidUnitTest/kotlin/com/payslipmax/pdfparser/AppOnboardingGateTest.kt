package com.payslipmax.pdfparser

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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppOnboardingGateTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    private class FakeOnboardingStorage(
        private var hasCompletedOnboarding: Boolean,
    ) : OnboardingStorage {
        override fun getHasCompletedOnboarding(): Boolean = hasCompletedOnboarding

        override fun saveHasCompletedOnboarding(completed: Boolean) {
            hasCompletedOnboarding = completed
        }

        override fun getHasSeenUploadCoachmark(): Boolean = false

        override fun saveHasSeenUploadCoachmark(seen: Boolean) {}
    }

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

    @Test
    fun showsOnboardingOverlayOnTopOfMainScaffoldWhenIncomplete() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()

            setContent {
                App(
                    viewModel = viewModel,
                    onPickPdf = { _ -> },
                    onOpenPdf = { _, _ -> },
                    onboardingManager = OnboardingManager(FakeOnboardingStorage(hasCompletedOnboarding = false)),
                )
            }
            testDispatcher.scheduler.runCurrent()

            // The onboarding card overlays MainScaffold rather than replacing it, so the real
            // Dashboard (and its FAB) is still composed underneath, dimly visible through the scrim.
            onNodeWithText(AppStringsOnboarding.onboardingSlide1Title).assertExists()
            onNodeWithTag("upload_fab").assertExists()
            // The upload coachmark must stay suppressed while the onboarding overlay is still
            // showing, even though the FakeOnboardingStorage above reports it as not-yet-seen —
            // otherwise both surfaces compete for attention on the very first Dashboard frame.
            onNodeWithTag("upload_coachmark").assertDoesNotExist()
        }

    @Test
    fun hidesOnboardingOverlayWhenAlreadyComplete() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()

            setContent {
                App(
                    viewModel = viewModel,
                    onPickPdf = { _ -> },
                    onOpenPdf = { _, _ -> },
                    onboardingManager = OnboardingManager(FakeOnboardingStorage(hasCompletedOnboarding = true)),
                )
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(AppStringsOnboarding.onboardingSlide1Title).assertDoesNotExist()
            onNodeWithTag("upload_fab").assertExists()
        }
}
