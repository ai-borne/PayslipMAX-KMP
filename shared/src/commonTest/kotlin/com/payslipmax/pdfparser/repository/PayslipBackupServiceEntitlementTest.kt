package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Phase 2 (D3): entitlement must never travel inside a backup file. [PayslipBackupService.restore] always
 * re-writes the *device's* own `isPremiumEnabled`, so a shared/premium backup can neither grant nor
 * revoke PRO on import (this path also backs cloud restore via `downloadAndRestoreBackup`).
 */
class PayslipBackupServiceEntitlementTest {
    private lateinit var repository: PayslipRepository
    private lateinit var service: PayslipBackupService

    @BeforeTest
    fun setUp() {
        val dao = FakePayslipDao()
        repository = PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined)
        service = PayslipBackupService(dao, Dispatchers.Unconfined)
    }

    @Test
    fun testImportPreservesDeviceEntitlementFreeStaysFree() =
        runTest {
            // Export a PREMIUM backup.
            repository.saveSettings(AppSettingsEntity(isPremiumEnabled = true))
            val backup = service.export("pwd").getOrThrow()

            // Device is FREE at import time.
            repository.clearSettings()
            repository.saveSettings(AppSettingsEntity(isPremiumEnabled = false))

            service.restore(backup, "pwd").getOrThrow()

            // Restoring a premium backup must NOT grant PRO to a free device.
            assertFalse(repository.getSettings()!!.isPremiumEnabled)
        }

    @Test
    fun testImportPreservesDeviceEntitlementPremiumStaysPremium() =
        runTest {
            // Export a FREE backup.
            repository.saveSettings(AppSettingsEntity(isPremiumEnabled = false))
            val backup = service.export("pwd").getOrThrow()

            // Device is PREMIUM at import time.
            repository.clearSettings()
            repository.saveSettings(AppSettingsEntity(isPremiumEnabled = true))

            service.restore(backup, "pwd").getOrThrow()

            // Restoring a free backup must NOT revoke PRO from a premium device.
            assertTrue(repository.getSettings()!!.isPremiumEnabled)
        }
}
