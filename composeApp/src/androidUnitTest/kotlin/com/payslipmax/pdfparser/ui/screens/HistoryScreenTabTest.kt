package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.FakeFinancialIntelligenceRepository
import com.payslipmax.pdfparser.ui.PayslipViewModel
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
import kotlin.test.assertEquals

/**
 * Verifies History screen renders payslip statements and responds correctly to user interactions.
 * AI Reports tab removed — history now shows statements only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryScreenTabTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
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
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        financialRepository = FakeFinancialIntelligenceRepository(fakeDao)
        viewModel = PayslipViewModel(repository, financialRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    /**
     * Verifies the history screen renders without crash when payslip list is empty.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testEmptyHistoryScreenRendersWithoutCrash() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()
            setContent {
                HistoryScreen(viewModel = viewModel, onOpenPdf = { _, _ -> }, onNavigateToInsights = {})
            }
            waitForIdle()
            // Empty state — no crash, composable renders.
        }

    /**
     * Verifies tapping a payslip row fires onOpenPayslipDetail with the correct payslip.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTapPayslipRowInvokesOnOpenPayslipDetail() =
        runComposeUiTest {
            val payslip = buildPayslip(year = 2024, monthNum = 8, monthName = "August")
            runBlocking { fakeDao.insertPayslip(payslip.toEncryptedEntity()) }
            testDispatcher.scheduler.runCurrent()

            var openedPayslip: ParsedPayslip? = null
            setContent {
                HistoryScreen(
                    viewModel = viewModel,
                    onOpenPdf = { _, _ -> },
                    onNavigateToInsights = {},
                    onOpenPayslipDetail = { openedPayslip = it },
                )
            }
            testDispatcher.scheduler.runCurrent()

            // Most recent payslip's year auto-expands on load, so the row is directly accessible.
            onNodeWithText("August").performClick()
            waitForIdle()

            assertEquals("08/2024", openedPayslip?.dateStr)
        }
}
