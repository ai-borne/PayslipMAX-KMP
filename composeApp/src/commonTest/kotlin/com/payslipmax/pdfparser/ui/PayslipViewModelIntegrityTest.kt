package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.domain.AppIntegrityStatus
import com.payslipmax.pdfparser.domain.FakeAppIntegrityChecker
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelIntegrityTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_initializesWithValidIntegrityStatus() =
        runTest {
            val fakeChecker = FakeAppIntegrityChecker(AppIntegrityStatus.Valid)
            val repository = PayslipRepository(FakePayslipDao(), FakePdfParser())
            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    appIntegrityChecker = fakeChecker,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(AppIntegrityStatus.Valid, viewModel.uiState.value.appIntegrityStatus)
            assertTrue(viewModel.uiState.value.appIntegrityStatus.isAllowedToRun)
        }

    @Test
    fun viewModel_updatesUiStateToSideloaded_whenCheckerDetectsSideloading() =
        runTest {
            val sideloadReason = "APK installed from unknown web source"
            val fakeChecker = FakeAppIntegrityChecker(AppIntegrityStatus.Sideloaded(sideloadReason))
            val repository = PayslipRepository(FakePayslipDao(), FakePdfParser())
            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    appIntegrityChecker = fakeChecker,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            val status = viewModel.uiState.value.appIntegrityStatus
            assertTrue(status is AppIntegrityStatus.Sideloaded)
            assertEquals(sideloadReason, (status as AppIntegrityStatus.Sideloaded).reason)
        }
}
