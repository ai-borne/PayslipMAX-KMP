package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelCorrectionSessionTest {
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
                    earnings = Earnings(basicPay = 100.0, dearnessAllowance = 50.0),
                    deductions = Deductions(incomeTax = 10.0),
                    ledgerBalances = LedgerBalances(),
                    summary = PayslipSummary(150.0, 10.0, 140.0),
                    taxAndSavings = null,
                ),
            )
        repository = PayslipRepository(FakePayslipDao(), fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartSessionLoadsExistingCorrections() =
        runTest {
            viewModel.importPayslip(byteArrayOf(1), "pass", "08-2024.pdf")

            // Save initial correction directly
            repository.saveCorrection("08/2024", "basicPay", 120.0)

            viewModel.startEditingSession("08/2024")

            val state = viewModel.uiState.value
            assertTrue(state.isEditModeActive)
            assertEquals(1, state.draftCorrections.size)
            assertEquals(120.0, state.draftCorrections["basicPay"]?.amount)
        }

    @Test
    fun testUpdateDraftAccumulatesChanges() =
        runTest {
            viewModel.importPayslip(byteArrayOf(1), "pass", "08-2024.pdf")
            viewModel.startEditingSession("08/2024")

            val corr =
                SingleCorrection(
                    fieldKey = "dearnessAllowance",
                    codeHead = "DA",
                    amount = 75.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.EDITED,
                    originalAmount = 50.0,
                    originalCodeHead = "DA",
                    timestamp = 100L,
                )
            viewModel.updateDraftCorrection(corr)

            val state = viewModel.uiState.value
            assertEquals(75.0, state.draftCorrections["dearnessAllowance"]?.amount)

            // Database is untouched until save
            val dbMerged = repository.getPayslipByDate("08/2024")!!
            assertEquals(50.0, dbMerged.earnings.dearnessAllowance)
        }

    @Test
    fun testCancelDiscardsDraftSession() =
        runTest {
            viewModel.importPayslip(byteArrayOf(1), "pass", "08-2024.pdf")
            viewModel.startEditingSession("08/2024")

            val corr =
                SingleCorrection(
                    fieldKey = "dearnessAllowance",
                    codeHead = "DA",
                    amount = 75.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.EDITED,
                    originalAmount = 50.0,
                    originalCodeHead = "DA",
                    timestamp = 100L,
                )
            viewModel.updateDraftCorrection(corr)
            viewModel.cancelEditingSession()

            val state = viewModel.uiState.value
            assertFalse(state.isEditModeActive)
            assertTrue(state.draftCorrections.isEmpty())
        }

    @Test
    fun testSaveSessionCommitsAndRefreshes() =
        runTest {
            viewModel.importPayslip(byteArrayOf(1), "pass", "08-2024.pdf")
            viewModel.startEditingSession("08/2024")

            val corr =
                SingleCorrection(
                    fieldKey = "dearnessAllowance",
                    codeHead = "DA",
                    amount = 75.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.EDITED,
                    originalAmount = 50.0,
                    originalCodeHead = "DA",
                    timestamp = 100L,
                )
            viewModel.updateDraftCorrection(corr)
            viewModel.saveEditingSession("08/2024")

            val state = viewModel.uiState.value
            assertFalse(state.isEditModeActive)
            assertTrue(state.draftCorrections.isEmpty())

            // Database and selected payslip state reflect updates
            assertEquals(75.0, state.selectedPayslip?.earnings?.dearnessAllowance)
            val dbMerged = repository.getPayslipByDate("08/2024")!!
            assertEquals(75.0, dbMerged.earnings.dearnessAllowance)
        }
}
