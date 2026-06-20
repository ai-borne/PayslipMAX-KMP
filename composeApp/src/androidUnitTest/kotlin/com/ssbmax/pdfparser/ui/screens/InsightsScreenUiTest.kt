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
        org.koin.core.context.stopKoin()
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
            val officer = Officer("Test Officer", "00/000/000000X", "AA****00A")
            val summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0)
            val aprilPayslip = ParsedPayslip(
                file = "test.pdf", year = 2026, monthNum = 4, monthName = "April",
                dateStr = "04/2026", officer = officer, earnings = Earnings(basicPay = 100000.0),
                deductions = Deductions(), ledgerBalances = LedgerBalances(), summary = summary, taxAndSavings = null,
            )
            val marchPayslip = ParsedPayslip(
                file = "test.pdf", year = 2026, monthNum = 3, monthName = "March",
                dateStr = "03/2026", officer = officer, earnings = Earnings(basicPay = 100000.0),
                deductions = Deductions(), ledgerBalances = LedgerBalances(), summary = summary, taxAndSavings = null,
            )
            runBlocking {
                fakeDao.insertPayslip(marchPayslip.toEncryptedEntity())
                fakeDao.insertPayslip(aprilPayslip.toEncryptedEntity())
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
            val officer = Officer("Test Officer", "00/000/000000X", "AA****00A")
            val summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0)
            val aprilPayslip = ParsedPayslip(
                file = "test.pdf", year = 2026, monthNum = 4, monthName = "April",
                dateStr = "04/2026", officer = officer, earnings = Earnings(basicPay = 100000.0),
                deductions = Deductions(), ledgerBalances = LedgerBalances(), summary = summary, taxAndSavings = null,
            )
            runBlocking {
                fakeDao.insertPayslip(aprilPayslip.toEncryptedEntity())
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
            val officer = Officer("Test Officer", "00/000/000000X", "AA****00A")
            val summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0)
            val aprilPayslip = ParsedPayslip(
                file = "test.pdf", year = 2026, monthNum = 4, monthName = "April",
                dateStr = "04/2026", officer = officer, earnings = Earnings(basicPay = 100000.0),
                deductions = Deductions(), ledgerBalances = LedgerBalances(), summary = summary, taxAndSavings = null,
            )
            runBlocking {
                fakeDao.insertPayslip(aprilPayslip.toEncryptedEntity())
            }
            viewModel.setPremiumEnabled(true)
            testDispatcher.scheduler.runCurrent()
            setContent { InsightsScreen(viewModel = viewModel, onNavigateTo = {}) }
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            // ProFeaturesTeaser must not appear for premium users
            onNodeWithText(AppStrings.settingsProUpgradeBtn).assertDoesNotExist()
        }
}
