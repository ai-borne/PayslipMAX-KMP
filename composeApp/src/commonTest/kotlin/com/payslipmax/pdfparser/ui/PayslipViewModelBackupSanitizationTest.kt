package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.theme.AppStrings
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
import kotlin.test.assertTrue

/**
 * Phase 2 (D3): a whole-DB restore must never carry PRO entitlement in or out — [restoreDatabase]
 * re-writes the device's own pre-restore `isPremiumEnabled` after the swap. These tests simulate the
 * file swap via [FakeBackupManager.onRestoreSideEffect], which mutates the DAO like the real actuals.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelBackupSanitizationTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeBackupManager = FakeBackupManager()
        repository = PayslipRepository(fakeDao, FakePdfParser(), Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository, fakeBackupManager)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRestorePremiumBackupOntoFreeDeviceStaysFree() =
        runTest {
            // Device is FREE.
            fakeDao.insertSettings(AppSettingsEntity(isPremiumEnabled = false))
            runCurrent()
            // The imported backup is PREMIUM (simulated by the swap side effect).
            fakeBackupManager.onRestoreSideEffect = {
                fakeDao.insertSettings(AppSettingsEntity(isPremiumEnabled = true))
            }

            var callback: Result<Unit>? = null
            viewModel.restoreDatabase("pwd") { callback = it }
            runCurrent()

            assertTrue(callback!!.isSuccess)
            // Device must stay free — a shared premium backup can't grant PRO.
            assertFalse(repository.getSettings()!!.isPremiumEnabled)
            assertFalse(viewModel.uiState.value.isPremiumEnabled)
        }

    @Test
    fun testRestoreFreeBackupOntoPremiumDeviceStaysPremium() =
        runTest {
            // Device is PREMIUM.
            fakeDao.insertSettings(AppSettingsEntity(isPremiumEnabled = true))
            runCurrent()
            // The imported backup is FREE.
            fakeBackupManager.onRestoreSideEffect = {
                fakeDao.insertSettings(AppSettingsEntity(isPremiumEnabled = false))
            }

            var callback: Result<Unit>? = null
            viewModel.restoreDatabase("pwd") { callback = it }
            runCurrent()

            assertTrue(callback!!.isSuccess)
            // Device must stay premium — a free backup can't revoke PRO.
            assertTrue(repository.getSettings()!!.isPremiumEnabled)
            assertTrue(viewModel.uiState.value.isPremiumEnabled)
        }

    @Test
    fun testRestoreBackupFailureLeavesEntitlementUnchanged() =
        runTest {
            fakeDao.insertSettings(AppSettingsEntity(isPremiumEnabled = true))
            runCurrent()
            fakeBackupManager.restoreResult = Result.failure(Exception("bad password"))

            var callback: Result<Unit>? = null
            viewModel.restoreDatabase("pwd") { callback = it }
            runCurrent()

            assertTrue(callback!!.isFailure)
            assertTrue(repository.getSettings()!!.isPremiumEnabled)
        }

    @Test
    fun testSanitizeWriteFailureFailsLoud() =
        runTest {
            // Device is FREE.
            fakeDao.insertSettings(AppSettingsEntity(isPremiumEnabled = false))
            runCurrent()
            // Restore succeeds and imports a PREMIUM flag (allowed through)...
            fakeBackupManager.onRestoreSideEffect = {
                fakeDao.insertSettings(AppSettingsEntity(isPremiumEnabled = true))
            }
            // ...but the sanitizing write-back (device entitlement = free) is made to fail.
            fakeDao.failInsertSettingsWhen = { !it.isPremiumEnabled }

            var callback: Result<Unit>? = null
            viewModel.restoreDatabase("pwd") { callback = it }
            runCurrent()

            // Fail loud: the caller sees failure and the UI surfaces an error state.
            assertTrue(callback!!.isFailure)
            assertEquals(AppStrings.errorRestoreSanitizeFailed, viewModel.uiState.value.error)
        }
}
