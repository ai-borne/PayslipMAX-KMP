package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
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
import com.payslipmax.pdfparser.ui.FakeBackupManager
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Run on API 34 to match local SDK compatibility
class DashboardScreenUiTest {
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
    fun testLoadingState() =
        runComposeUiTest {
            setContent {
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdfTrigger = {},
                )
            }

            // Verify the progress spinner (dashboard_loading tag) exists
            onNodeWithTag("dashboard_loading").assertExists()
            // Verify other states are not shown
            onNodeWithTag("dashboard_empty").assertDoesNotExist()
            onNodeWithTag("dashboard_populated").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testEmptyState() =
        runComposeUiTest {
            // Advance the scheduler to let the viewModel's init/loading coroutines run and finish
            testDispatcher.scheduler.runCurrent()

            setContent {
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdfTrigger = {},
                )
            }

            // Verify the empty state placeholder (dashboard_empty tag) exists
            onNodeWithTag("dashboard_empty").assertExists()
            // Verify loading indicator is gone
            onNodeWithTag("dashboard_loading").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testTaxRateCardRoundsCleanlyWithoutTrailingDot() =
        runComposeUiTest {
            // grossPay == incomeTax makes the rate exactly 100.0 -- the old
            // `taxRate.toString().take(4)` truncation rendered this as "100.%".
            val payslip =
                ParsedPayslip(
                    file = "test.pdf",
                    year = 2026,
                    monthNum = 4,
                    monthName = "April",
                    dateStr = "04/2026",
                    officer = Officer("Test Officer", "00/000/000000X", "AA****00A"),
                    earnings = Earnings(basicPay = 500.0),
                    deductions = Deductions(incomeTax = 500.0),
                    ledgerBalances = LedgerBalances(),
                    summary = PayslipSummary(grossPay = 500.0, totalDeductions = 500.0, netRemittance = 0.0),
                    taxAndSavings = null,
                )
            runBlocking { fakeDao.insertPayslip(payslip.toEncryptedEntity()) }
            testDispatcher.scheduler.runCurrent()

            setContent {
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdfTrigger = {},
                )
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText("100.0%").assertExists()
            onNodeWithText("100.%").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testAllocationChartCardNotObscuredByUploadFab() =
        runComposeUiTest {
            val payslip =
                ParsedPayslip(
                    file = "test.pdf",
                    year = 2026,
                    monthNum = 4,
                    monthName = "April",
                    dateStr = "04/2026",
                    officer = Officer("Test Officer", "00/000/000000X", "AA****00A"),
                    earnings = Earnings(basicPay = 50000.0),
                    deductions = Deductions(incomeTax = 5000.0),
                    ledgerBalances = LedgerBalances(),
                    summary = PayslipSummary(grossPay = 60000.0, totalDeductions = 10000.0, netRemittance = 50000.0),
                    taxAndSavings = null,
                )
            runBlocking { fakeDao.insertPayslip(payslip.toEncryptedEntity()) }
            testDispatcher.scheduler.runCurrent()

            setContent {
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdfTrigger = {},
                )
            }
            testDispatcher.scheduler.runCurrent()

            // Swipe to the true end of scroll -- not just far enough to bring the card into view --
            // so the trailing FAB-clearance space is actually scrolled past.
            onRoot().performTouchInput { swipeUp() }
            mainClock.advanceTimeBy(300)

            val cardBottom = onNodeWithTag("allocation_chart_card").getUnclippedBoundsInRoot().bottom
            val fabTop = onNodeWithTag("upload_fab").getUnclippedBoundsInRoot().top

            // The FAB is overlaid on top of the scrollable content, so the last card's bottom edge
            // must sit above the FAB's top edge once fully scrolled -- otherwise the FAB permanently
            // covers the bottom of the card's figures.
            assert(cardBottom <= fabTop) {
                "Expected allocation card bottom ($cardBottom) to be above the FAB top ($fabTop), but it overlaps"
            }
        }
}
