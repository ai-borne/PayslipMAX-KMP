package com.ssbmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.ssbmax.pdfparser.database.toEncryptedEntity
import com.ssbmax.pdfparser.domain.*
import com.ssbmax.pdfparser.repository.PayslipRepository
import com.ssbmax.pdfparser.testing.FakePayslipDao
import com.ssbmax.pdfparser.testing.FakePdfParser
import com.ssbmax.pdfparser.ui.FakeBackupManager
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.setPremiumEnabled
import com.ssbmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
class InsightsScreenUiTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    private val testOfficer = Officer("Test Officer", "00/000/000000X", "AA****00A")
    private val testSummary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0)

    private fun buildPayslip(year: Int, monthNum: Int, monthName: String) = ParsedPayslip(
        file = "test.pdf", year = year, monthNum = monthNum, monthName = monthName,
        dateStr = "${monthNum.toString().padStart(2, '0')}/$year",
        officer = testOfficer,
        earnings = Earnings(basicPay = 100000.0),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = testSummary,
        taxAndSavings = null,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        fakeBackupManager = FakeBackupManager()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository, fakeBackupManager)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {}
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testEmptyState() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()
            setContent {
                InsightsScreen(
                    viewModel = viewModel,
                    onNavigateTo = {},
                )
            }
            onNodeWithText("Please import or select a payslip to unlock financial insights.").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testMonthDropdownOpensOnTap() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 3, "March").toEncryptedEntity())
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            testDispatcher.scheduler.runCurrent()
            setContent {
                InsightsScreen(viewModel = viewModel, onNavigateTo = {})
            }
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            // April 2026 is the most recent and should be auto-selected
            onNode(hasText("April 2026") and isSelected()).assertIsDisplayed()
            // First tap — dropdown must open (FilterChip is the selected node)
            onNode(hasText("April 2026") and isSelected()).performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("March 2026").assertIsDisplayed()
            // Second tap — dropdown must close
            onNode(hasText("April 2026") and isSelected()).performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("March 2026").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun freeUserSeesProTeaserAtBottom_notInlineLockedCards() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            testDispatcher.scheduler.runCurrent()
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            // Inline locked card removed for free users — its unique CTA must not exist
            onNodeWithText(AppStrings.aiAuditUnlockBtn).assertDoesNotExist()
            // ProFeaturesTeaser is at scroll bottom — swipe to compose it, then assert
            onRoot().performTouchInput { swipeUp() }
            mainClock.advanceTimeBy(300)
            onNodeWithText(AppStrings.settingsProUpgradeBtn).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun premiumUserSeesCaReportInlineNotTeaser() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertPayslip(buildPayslip(2026, 4, "April").toEncryptedEntity())
            }
            viewModel.setPremiumEnabled(true)
            testDispatcher.scheduler.advanceUntilIdle()
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.advanceUntilIdle()
            mainClock.advanceTimeBy(300)

            // ProFeaturesTeaser must not appear for premium users
            onNodeWithText(AppStrings.settingsProUpgradeBtn).assertDoesNotExist()

            // Scroll to compose the section in LazyColumn
            onNode(hasScrollAction()).performScrollToNode(hasText(AppStrings.geminiAiAnalyzeBtn))
            mainClock.advanceTimeBy(300)

            // CA report active card must be present (its generate CTA is the positive signal)
            onNodeWithText(AppStrings.geminiAiAnalyzeBtn).assertExists()
        }
}
