package com.ssbmax.pdfparser.ui

import com.ssbmax.pdfparser.database.*
import com.ssbmax.pdfparser.domain.*
import com.ssbmax.pdfparser.repository.PayslipRepository
import com.ssbmax.pdfparser.testing.FakePayslipDao
import com.ssbmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelSettingsTest {
    private val testDispatcher = UnconfinedTestDispatcher()

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
    }

    @Test
    fun testBackupDatabaseSuccess() =
        runTest {
            fakeBackupManager.backupResult = Result.success(Unit)
            var callbackResult: Result<Unit>? = null

            viewModel.backupDatabase("pass") { callbackResult = it }

            assertEquals(1, fakeBackupManager.backupCalledCount)
            assertNotNull(callbackResult)
            assertTrue(callbackResult!!.isSuccess)
        }

    @Test
    fun testRestoreDatabaseSuccess() =
        runTest {
            fakeBackupManager.restoreResult = Result.success(Unit)
            var callbackResult: Result<Unit>? = null

            val mock = createMockPayslip("08/2024")
            fakeDao.insertPayslip(mock.toEncryptedEntity())

            viewModel.restoreDatabase("pass") { callbackResult = it }

            assertEquals(1, fakeBackupManager.restoreCalledCount)
            assertNotNull(callbackResult)
            assertTrue(callbackResult!!.isSuccess)
        }

    @Test
    fun testSettingsChanges() =
        runTest {
            viewModel.setPremiumEnabled(true)
            viewModel.setAppTheme("dark")
            viewModel.updateProfileOverrides("Test Officer", "CDA123", "PAN123")
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state.isPremiumEnabled)
            assertEquals("dark", state.appTheme)
            assertEquals("Test Officer", state.profileName)
            assertEquals("CDA123", state.profileCdaNumber)
            assertEquals("PAN123", state.profilePanNumber)
        }

    @Test
    fun testPasscodeLock() =
        runTest {
            viewModel.setLockEnabled(true, "1234")
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state.isLockEnabled)
            assertFalse(state.isAppLocked)

            val coldViewModel = PayslipViewModel(repository, fakeBackupManager)
            runCurrent()
            assertTrue(coldViewModel.uiState.value.isLockEnabled)
            assertTrue(coldViewModel.uiState.value.isAppLocked)

            assertFalse(coldViewModel.verifyPin("0000"))
            assertTrue(coldViewModel.uiState.value.isAppLocked)

            assertTrue(coldViewModel.verifyPin("1234"))
            assertFalse(coldViewModel.uiState.value.isAppLocked)

            coldViewModel.lockApp()
            assertTrue(coldViewModel.uiState.value.isAppLocked)

            coldViewModel.unlockApp()
            assertFalse(coldViewModel.uiState.value.isAppLocked)
        }

    private fun createMockPayslip(dateStr: String) =
        dateStr.split("/").let { split ->
            val month = split[0].toInt()
            val year = split[1].toInt()
            ParsedPayslip(
                file = "payslip_$dateStr.pdf", year = year, monthNum = month, monthName = "Month_$month", dateStr = dateStr,
                officer = Officer("Name", "Acc", "PAN"),
                earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
                deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
                ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
                summary = PayslipSummary(100.0, 80.0, 20.0),
                taxAndSavings =
                    TaxAndSavings(
                        grossSalaryYtd = 1000.0,
                        totalTaxableIncome = 900.0,
                        standardDeduction = 50.0,
                        netTaxableIncome = 850.0,
                        totalTaxPayable = 100.0,
                        taxDeductedYtd = 80.0,
                        cessDeductedYtd = 20.0,
                        dsopFund = DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0),
                    ),
            )
        }

    @Test
    fun testResetPinWithPdfSuccess() =
        runTest {
            viewModel.setLockEnabled(true, "1234")
            viewModel.updateProfileOverrides("Test Officer", "CDA123", "PAN123")
            runCurrent()
            viewModel.lockApp()
            runCurrent()
            assertTrue(viewModel.uiState.value.isAppLocked)

            val mockPayslip = createMockPayslip("04/2026").let {
                it.copy(officer = it.officer.copy(pan = "PAN123"))
            }
            fakeParser.result = Result.success(mockPayslip)

            var callbackResult: Result<Unit>? = null
            viewModel.resetPinWithPdf(
                pdfBytes = byteArrayOf(1, 2, 3),
                password = "pass",
                filename = "04 Apr 2026.pdf"
            ) { callbackResult = it }
            runCurrent()

            assertNotNull(callbackResult)
            assertTrue(callbackResult!!.isSuccess)
            val state = viewModel.uiState.value
            assertFalse(state.isLockEnabled)
            assertFalse(state.isAppLocked)
            assertEquals("", state.appPinHash)
        }

    @Test
    fun testResetPinWithPdfPanMismatch() =
        runTest {
            viewModel.setLockEnabled(true, "1234")
            viewModel.updateProfileOverrides("Test Officer", "CDA123", "PAN123")
            runCurrent()
            viewModel.lockApp()
            runCurrent()
            assertTrue(viewModel.uiState.value.isAppLocked)

            val mockPayslip = createMockPayslip("04/2026").let {
                it.copy(officer = it.officer.copy(pan = "DIFFERENT_PAN"))
            }
            fakeParser.result = Result.success(mockPayslip)

            var callbackResult: Result<Unit>? = null
            viewModel.resetPinWithPdf(
                pdfBytes = byteArrayOf(1, 2, 3),
                password = "pass",
                filename = "04 Apr 2026.pdf"
            ) { callbackResult = it }
            runCurrent()

            assertNotNull(callbackResult)
            assertTrue(callbackResult!!.isFailure)
            assertEquals("PDF does not match the active user profile", callbackResult!!.exceptionOrNull()?.message)
            assertTrue(viewModel.uiState.value.isAppLocked)
        }
}
