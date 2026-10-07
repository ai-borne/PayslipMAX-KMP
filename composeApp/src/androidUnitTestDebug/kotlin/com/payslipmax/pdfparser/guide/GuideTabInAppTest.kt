package com.payslipmax.pdfparser.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.App
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakeOnboardingStorage
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.WithTestKoin
import com.payslipmax.pdfparser.testing.testAppModule
import com.payslipmax.pdfparser.ui.FakeFinancialIntelligenceRepository
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.lockApp
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.setLockEnabled
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.unlockApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Guide tab inside the real app shell, in a debug build where the dark-launch flag shows it. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideTabInAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PayslipViewModel
    private val guideNavState = GuideNavState()
    private val guideModule =
        module { single { GuideViewModel(FakeGuideRepository(), FakeCrashReporter(), UnconfinedTestDispatcher()) } }
    private val onboarding = OnboardingManager(FakeOnboardingStorage(hasCompletedOnboarding = true, hasSeenUploadCoachmark = true))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val dao = FakePayslipDao()
        viewModel = PayslipViewModel(PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined), FakeFinancialIntelligenceRepository(dao))
        composeRule.setContent {
            WithTestKoin(testAppModule(), guideModule) {
                App(viewModel = viewModel, onPickPdf = {}, onOpenPdf = { _, _ -> }, guideNavState = guideNavState, onboardingManager = onboarding)
            }
        }
        settle()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun settle() {
        testDispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun tap(text: String) {
        composeRule.onAllNodesWithText(text)[0].performClick()
        settle()
    }

    private fun openTravelArea() {
        tap(GuideStrings.tabLabel)
        tap("Travel")
        assertEquals(GuideDestination.Area("travel"), guideNavState.current)
    }

    @Test
    fun theGuideTabSitsBeforeSettingsAndOpensGuideHome() {
        tap(GuideStrings.tabLabel)
        composeRule.onNodeWithText(GuideStrings.homeSection).assertIsDisplayed()
        composeRule.onNodeWithText(AppStrings.navigationSettings).assertIsDisplayed()
    }

    @Test
    fun switchingTabsKeepsThePlaceInTheGuide() {
        openTravelArea()
        tap(AppStrings.navigationHistory)
        tap(GuideStrings.tabLabel)

        composeRule.onNodeWithText("Daily allowance on duty").assertIsDisplayed()
    }

    @Test
    fun reTappingTheActiveGuideTabGoesBackToGuideHome() {
        openTravelArea()
        tap(GuideStrings.tabLabel)

        assertEquals(GuideDestination.Home, guideNavState.current)
        composeRule.onNodeWithText(GuideStrings.homeSection).assertIsDisplayed()
    }

    @Test
    fun lockingHidesTheGuideAndUnlockingReturnsToTheSameLevel() {
        viewModel.setLockEnabled(true, pin = "1234")
        settle()
        openTravelArea()

        viewModel.lockApp()
        settle()
        composeRule.onNodeWithText("Daily allowance on duty").assertDoesNotExist()

        viewModel.unlockApp()
        settle()
        composeRule.onNodeWithText("Daily allowance on duty").assertIsDisplayed()
    }
}
