package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
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
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.selectHistoryDetailPayslip
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

/**
 * Pay Audit is Premium-only and entered solely from the Insights tools list, so the payslip detail screen
 * (free to view) must not carry an "Audit this month" shortcut. The replica table is asserted present
 * first so the absence cannot pass on an empty screen.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayslipReplicaHasNoPayAuditEntryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    private fun slip(month: Int) =
        ParsedPayslip(
            file = "t.pdf", year = 2024, monthNum = month, monthName = "M", dateStr = "0$month/2024",
            officer = Officer("Test Officer", "00/000/000000X", "AA****00A"),
            earnings = Earnings(basicPay = 100000.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0),
            taxAndSavings = null,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val dao = FakePayslipDao()
        val repository = PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined)
        runBlocking { listOf(7, 8).forEach { dao.insertPayslip(slip(it).toEncryptedEntity()) } }
        viewModel = PayslipViewModel(repository)
        testDispatcher.scheduler.runCurrent()
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
    fun payslipDetailOffersNoWayIntoPayAudit() =
        runComposeUiTest {
            viewModel.selectHistoryDetailPayslip("07/2024")
            testDispatcher.scheduler.runCurrent()
            setContent { PayslipReplicaDetailScreen(viewModel = viewModel, onBack = {}) }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText("Net Remittance (Take Home)").assertExists()
            onNodeWithText("Audit this month").assertDoesNotExist()
            onNodeWithText("See whether each pay line on this payslip is correct").assertDoesNotExist()
        }
}
