package com.payslipmax.pdfparser

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.FakeFinancialIntelligenceRepository
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.setPremiumEnabled
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppBackNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        val repository = PayslipRepository(fakeDao, FakePdfParser(), Dispatchers.Unconfined)
        viewModel =
            PayslipViewModel(
                repository,
                FakeFinancialIntelligenceRepository(fakeDao),
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private fun pressBack() =
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

    // --- Decision 9: state survives process death via the Saver ---
    // (pure-Saver-logic round-trip tests live in AppNavStateSaverTest.kt / commonTest — no
    // Robolectric/Compose UI host needed there; kept this file within its line budget.)

    // --- Complaint #3 + decision 7: system back at a tab root exits, not intercepted ---

    @Test
    fun backAtTabRootExitsApp() {
        composeRule.setContent {
            App(viewModel = viewModel, onPickPdf = { }, onOpenPdf = { _, _ -> })
        }
        composeRule.waitForIdle()
        // At the Dashboard tab root there is no enabled BackHandler, so back falls through to the
        // OS default (finish) — the app exits rather than navigating (decisions 7 and 3).
        assertFalse(composeRule.activity.isFinishing)
        pressBack()
        composeRule.waitForIdle()
        assertTrue(composeRule.activity.isFinishing)
    }

    // --- Complaint #3 + decision 8: a pushed detail hides the tab bar; back returns to its root ---

    @Test
    fun backFromPushedDetailReturnsToTabRootWithoutExiting() {
        composeRule.setContent {
            App(viewModel = viewModel, onPickPdf = { }, onOpenPdf = { _, _ -> })
        }
        composeRule.waitForIdle()

        // Switch to the Settings tab, then push the Help/Legal detail from it.
        composeRule.onAllNodesWithText(AppStrings.navigationSettings).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(AppStrings.settingsHelpFaqTitle).performScrollTo().performClick()
        composeRule.waitForIdle()

        // Decision 8: the bottom tab bar is hidden while a detail is pushed.
        composeRule.onNodeWithText(AppStrings.navigationHome).assertDoesNotExist()

        // Back pops the detail and returns to the Settings tab root — the app does not exit.
        pressBack()
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText(AppStrings.navigationHome).assertIsDisplayed()
    }

    // --- Phase 3: History's payslip detail is now a pushed Screen.PayslipReplica; mid-edit back
    // is two-step (decision 4) — first exits edit mode only, second returns to the list ---

    @Test
    fun backFromPushedPayslipReplicaIsTwoStepDuringEdit() {
        val payslip =
            ParsedPayslip(
                file = "test.pdf",
                year = 2024,
                monthNum = 8,
                monthName = "August",
                dateStr = "08/2024",
                officer = Officer("Test Officer", "00/000/000000X", "AA****00A"),
                earnings = Earnings(basicPay = 100000.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0),
                taxAndSavings = null,
            )
        runBlocking { fakeDao.insertPayslip(payslip.toEncryptedEntity()) }
        testDispatcher.scheduler.runCurrent()
        composeRule.waitForIdle()

        composeRule.setContent {
            App(viewModel = viewModel, onPickPdf = { }, onOpenPdf = { _, _ -> })
        }
        testDispatcher.scheduler.runCurrent()
        composeRule.waitForIdle()

        // Switch to History: the most recent payslip's year auto-expands on load
        // (observePayslips), so the row is reachable without an extra expand click; scroll it
        // into view first since the download banner pushes it below the fold.
        composeRule.onAllNodesWithText(AppStrings.navigationHistory).onFirst().performClick()
        testDispatcher.scheduler.runCurrent()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("August"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("August").performClick()
        composeRule.waitForIdle()

        // Decision 8: the bottom tab bar is hidden while the replica detail is pushed.
        composeRule.onNodeWithText(AppStrings.navigationHome).assertDoesNotExist()

        // Enter edit mode (startEditingSession is a suspend viewModelScope.launch, so the test
        // dispatcher needs an explicit pump), then press back: exits edit mode only (decision 4).
        composeRule.onNodeWithContentDescription("Start Editing").performScrollTo().performClick()
        testDispatcher.scheduler.runCurrent()
        composeRule.waitForIdle()
        assertTrue(viewModel.uiState.value.isEditModeActive)

        pressBack()
        composeRule.waitForIdle()
        assertFalse(viewModel.uiState.value.isEditModeActive)
        composeRule.onNodeWithText(AppStrings.navigationHome).assertDoesNotExist()

        // Second back — no longer editing — returns to the History list.
        pressBack()
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText(AppStrings.navigationHome).assertIsDisplayed()
    }

    // --- Phase 2: chained detail push (Settings -> PremiumFeatures -> TaxPlanning) must unwind one
    // level per back-press, not skip straight to the tab root (the reported regression) ---

    @Test
    fun backFromChainedPremiumFeaturesDetailReturnsToPremiumFeaturesNotSettings() {
        // Drive the real premium flag rather than relying on the debug-only FORCE_PRO override
        // (which the release unit test variant never applies), so Tax Planner is OPENABLE here
        // regardless of build type.
        viewModel.setPremiumEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        composeRule.setContent {
            App(viewModel = viewModel, onPickPdf = { }, onOpenPdf = { _, _ -> })
        }
        testDispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(AppStrings.navigationSettings).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(AppStringsPremium.premiumCatalogTitle).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(AppStringsPremium.premiumToolsTaxPlanner).performScrollTo().performClick()
        composeRule.waitForIdle()

        // On Tax Optimization Planner; back once must land on PremiumFeatures, not Settings.
        composeRule.onNodeWithText(AppStringsPremium.taxPlanningTitle).assertIsDisplayed()
        pressBack()
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText(AppStringsPremium.premiumCatalogSubtitle).assertIsDisplayed()
        composeRule.onNodeWithText(AppStrings.navigationHome).assertDoesNotExist()

        // Second back returns to the Settings tab root.
        pressBack()
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText(AppStrings.navigationHome).assertIsDisplayed()
    }
}
