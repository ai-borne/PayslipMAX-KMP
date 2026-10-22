package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.OVER_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.table
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [PayslipViewModel.leaveBankSummary] is the one derivation every Leave Bank surface reads. It must follow the payslip list
 * through every path that changes it (load, import, delete), or a surface would keep showing a balance the user just replaced
 * or deleted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelLeaveBankTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var repository: PayslipRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun summaryIsTheCalculatorsResultForTheStoredPayslips() =
        runTest {
            fakeDao.insertPayslip(payslip(2025, 12, table(OVER_CAP_DAYS)).toEncryptedEntity())
            fakeDao.insertPayslip(payslip(2026, 3, leave = null, basicPay = 160_000.0).toEncryptedEntity())
            val viewModel = PayslipViewModel(repository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.leaveBankSummary.collect {} }

            val summary = viewModel.leaveBankSummary.value

            assertEquals(OVER_CAP_DAYS, summary?.balanceDays)
            assertEquals(LeaveBankCalculator.summarize(viewModel.uiState.value.payslips), summary)
        }

    @Test
    fun importingANewerLeaveStatementReplacesTheBalance() =
        runTest {
            fakeDao.insertPayslip(payslip(2024, 12, table(282)).toEncryptedEntity())
            val viewModel = PayslipViewModel(repository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.leaveBankSummary.collect {} }
            assertEquals(282, viewModel.leaveBankSummary.value?.balanceDays)

            fakeParser.result = Result.success(payslip(2025, 12, table(OVER_CAP_DAYS)))
            viewModel.importPayslip("dummyBytes".encodeToByteArray(), "", "dec2025.pdf")

            assertEquals(OVER_CAP_DAYS, viewModel.leaveBankSummary.value?.balanceDays)
        }

    @Test
    fun deletingTheOnlyLeaveStatementClearsTheSummary() =
        runTest {
            fakeDao.insertPayslip(payslip(2025, 12, table(OVER_CAP_DAYS)).toEncryptedEntity())
            fakeDao.insertPayslip(payslip(2026, 1, leave = null).toEncryptedEntity())
            val viewModel = PayslipViewModel(repository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.leaveBankSummary.collect {} }
            assertEquals(OVER_CAP_DAYS, viewModel.leaveBankSummary.value?.balanceDays)

            viewModel.deletePayslip("12/2025")

            assertNull(viewModel.leaveBankSummary.value)
        }
}
