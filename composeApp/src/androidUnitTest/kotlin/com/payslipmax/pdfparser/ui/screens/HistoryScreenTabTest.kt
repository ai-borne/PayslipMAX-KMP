package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.payslipmax.pdfparser.database.AiInsightReportEntity
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.FakeBackupManager
import com.payslipmax.pdfparser.ui.FakeFinancialIntelligenceRepository
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.theme.AppStrings
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
class HistoryScreenTabTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var financialRepository: FakeFinancialIntelligenceRepository
    private lateinit var viewModel: PayslipViewModel

    private val testOfficer = Officer("Test Officer", "00/000/000000X", "AA****00A")
    private val testSummary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0)

    private fun buildPayslip(
        year: Int,
        monthNum: Int,
        monthName: String,
    ) = ParsedPayslip(
        file = "test.pdf",
        year = year,
        monthNum = monthNum,
        monthName = monthName,
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
        financialRepository = FakeFinancialIntelligenceRepository(fakeDao)
        viewModel = PayslipViewModel(repository, fakeBackupManager, financialRepository)
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
    fun testTabSelectorRendersCorrectly() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()
            setContent {
                HistoryScreen(viewModel = viewModel, onOpenPdf = { _, _ -> }, onNavigateToInsights = {})
            }
            onNodeWithText(AppStrings.historyTabStatements).assertExists()
            onNodeWithText(AppStrings.historyTabAiReports).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testEmptySavedAiReportsState() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()
            setContent {
                HistoryScreen(viewModel = viewModel, onOpenPdf = { _, _ -> }, onNavigateToInsights = {})
            }
            // Navigate to AI Reports tab
            onNodeWithText(AppStrings.historyTabAiReports).performClick()
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(100)

            onNodeWithText(AppStrings.historyEmptyAiReports).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSavedAiReportsListAndBottomSheetOpen() =
        runComposeUiTest {
            runBlocking {
                fakeDao.insertAiInsightReport(
                    AiInsightReportEntity(
                        id = "report_1",
                        payslipMonth = "02/2026",
                        generatedDate = 1718919600000L,
                        reportJSON = "This is a **Premium Narrative** report for Feb 2026",
                        reportVersion = "1.0",
                    ),
                )
            }
            testDispatcher.scheduler.runCurrent()
            setContent {
                HistoryScreen(viewModel = viewModel, onOpenPdf = { _, _ -> }, onNavigateToInsights = {})
            }

            // Switch to AI Reports tab
            onNodeWithText(AppStrings.historyTabAiReports).performClick()
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(100)

            // Month text for report: "February 2026"
            onNodeWithText("February 2026").assertIsDisplayed()
            onNodeWithText("Premium Intelligence Narrative").assertIsDisplayed()

            // Tap the report to open bottom sheet
            onNodeWithText("February 2026").performClick()
            testDispatcher.scheduler.runCurrent()
            mainClock.advanceTimeBy(300)

            // Verify bottom sheet title
            onNodeWithText(AppStrings.settingsAiInsightsLockedTitle).assertIsDisplayed()
            // Verify content is parsed
            onNodeWithText("This is a Premium Narrative report for Feb 2026", substring = true).assertIsDisplayed()

            // Verify there is no refresh/regenerate button (onRegenerateClick is null)
            onNodeWithText("🔄").assertDoesNotExist()
        }
}
