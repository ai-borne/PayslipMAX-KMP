package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.theme.AppStrings
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

/**
 * WHY (docs/Plan/14a P7, P10 "From P6" item 8 and "From P7" item 3): a failed delete, seed or read used to put the exception's own message
 * into the state, and a storage message can carry a path or a row. The state now holds one fixed line from the strings SSOT, whatever
 * the failure said.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelDataErrorTest {
    private val dao = FakePayslipDao()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = PayslipViewModel(PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined))
        dao.failClearAllWith = IllegalStateException("/data/user/0/in.aiborne.payslipmax/databases/payslips.db ABCDE1234F 98765.43")
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aDeleteThatFailsShowsTheFixedLineNotTheExceptionsMessage() =
        runTest {
            viewModel.clearAllData()

            assertEquals(AppStrings.errorClearDataFailed, viewModel.uiState.value.error)
        }

    @Test
    fun aSeedThatFailsShowsTheFixedLineNotTheExceptionsMessage() =
        runTest {
            viewModel.seedMockData()

            assertEquals(AppStrings.errorSeedDataFailed, viewModel.uiState.value.error)
        }

    @Test
    fun aPayslipReadThatBreaksShowsTheFixedLineNotTheExceptionsMessage() =
        runTest {
            dao.failGetAllPayslipsWith = IllegalStateException("/data/user/0/in.aiborne.payslipmax/databases/payslips.db ABCDE1234F 98765.43")

            viewModel.observePayslips()

            assertEquals(AppStrings.errorLoadPayslipsFailed, viewModel.uiState.value.error)
        }
}
