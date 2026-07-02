package com.ssbmax.pdfparser.ui

import com.ssbmax.pdfparser.domain.Deductions
import com.ssbmax.pdfparser.domain.Earnings
import com.ssbmax.pdfparser.domain.LedgerBalances
import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.domain.PayslipSummary
import com.ssbmax.pdfparser.repository.PayslipRepository
import com.ssbmax.pdfparser.testing.FakePayslipDao
import com.ssbmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Phase 5 — ViewModel correction flow: persisting a correction reflects the merged value in state. */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelCorrectionTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val fakeParser = FakePdfParser()
        fakeParser.result =
            Result.success(
                ParsedPayslip(
                    file = "08-2024.pdf",
                    year = 2024,
                    monthNum = 8,
                    monthName = "August",
                    dateStr = "08/2024",
                    officer = Officer("Name", "Acc", "PAN"),
                    earnings = Earnings(basicPay = 100.0),
                    deductions = Deductions(incomeTax = 10.0),
                    ledgerBalances = LedgerBalances(),
                    summary = PayslipSummary(100.0, 10.0, 90.0),
                    taxAndSavings = null,
                ),
            )
        repository = PayslipRepository(FakePayslipDao(), fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository, FakeBackupManager())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testApplyCorrectionUpdatesSelectedAndListedPayslip() =
        runTest {
            viewModel.importPayslip(byteArrayOf(1), "pass", "08-2024.pdf")

            viewModel.applyCorrection("08/2024", "basicPay", 999.0)

            val state = viewModel.uiState.value
            assertEquals(999.0, state.selectedPayslip?.earnings?.basicPay)
            assertEquals(999.0, state.payslips.first { it.dateStr == "08/2024" }.earnings.basicPay)
        }
}
