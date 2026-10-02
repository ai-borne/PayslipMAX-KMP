package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.hexToByteArray
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A restored payslip is re-encrypted under the restoring device's own key, never left under the backup password. */
class PayslipBackupServiceDeviceKeyTest {
    @Test
    fun testUniversalBackupEncryptsWithBackupPasswordAndImportsToDeviceKey() =
        runTest {
            val fakeDao = FakePayslipDao()
            val fakeParser = FakePdfParser()
            val repository = PayslipRepository(fakeDao, fakeParser, kotlinx.coroutines.Dispatchers.Unconfined)
            val service = PayslipBackupService(fakeDao, kotlinx.coroutines.Dispatchers.Unconfined)

            val payslip = createMockPayslip("08/2026")
            fakeParser.result = Result.success(payslip)

            // Import payslip (which encrypts it with current device key)
            repository.importPayslip(byteArrayOf(1, 2, 3), "pdf-password", "payslip.pdf")

            // Export backup using a backup password
            val backupPwd = "MyBackupPassword123!"
            val exportResult = service.export(backupPwd)
            assertTrue(exportResult.isSuccess)
            val backupBytes = exportResult.getOrThrow()

            // Clear the DAO to simulate restore on a new/clean device
            repository.clearAll()
            assertTrue(repository.getAllPayslips().first().isEmpty())

            // Restore backup using the backup password
            val importResult = service.restore(backupBytes, backupPwd)
            assertTrue(importResult.isSuccess)

            // Verify that the restored payslip in the DB is encrypted with the device key
            val dbEntities = fakeDao.getAllPayslips().first()
            assertEquals(1, dbEntities.size)

            val restoredEntity = dbEntities.first()
            val restoredCiphertextBytes = restoredEntity.ciphertext.hexToByteArray()

            // Should be decryptable by the device key
            val deviceKey = CryptoHelper.getDatabaseSecretKey()
            val decryptDeviceResult = CryptoHelper.decrypt(restoredCiphertextBytes, deviceKey)
            assertTrue(decryptDeviceResult.isSuccess, "Restored entity should be encrypted with the device key")

            // Should NOT be decryptable by the backup password directly (since the DB entity uses device key)
            val decryptBackupPwdResult = CryptoHelper.decrypt(restoredCiphertextBytes, backupPwd)
            assertTrue(decryptBackupPwdResult.isFailure, "Restored entity in DB must not be decryptable using backup password directly")
        }

    @Test
    fun testUniversalRestoreFromDifferentDeviceSucceeds() =
        runTest {
            val fakeDao = FakePayslipDao()
            val fakeParser = FakePdfParser()
            val repository = PayslipRepository(fakeDao, fakeParser, kotlinx.coroutines.Dispatchers.Unconfined)
            val service = PayslipBackupService(fakeDao, kotlinx.coroutines.Dispatchers.Unconfined)

            val payslip = createMockPayslip("08/2026")

            // Simulating Device A:
            // When Device A exports a backup, the individual payslips are encrypted using the backup password
            val backupPwd = "BackupPassword"
            val encryptedEntity = payslip.toEncryptedEntity(backupPwd)

            // Simulating Device A creating a backup encrypted with backup password "BackupPassword"
            val backup =
                PortableBackup(
                    version = 2,
                    encryptedPayslips = listOf(encryptedEntity),
                    pdfs = emptyList(),
                    settings = null,
                )
            val jsonStr = kotlinx.serialization.json.Json.encodeToString(PortableBackup.serializer(), backup)
            val encryptedBackupBytes = CryptoHelper.encrypt(jsonStr.encodeToByteArray(), backupPwd).getOrThrow()

            // Now, we are on Device B. Device B uses its own unique key.
            // We restore the backup.
            // This MUST succeed, and the database record on Device B MUST be decryptable by Device B's key!
            val restoreResult = service.restore(encryptedBackupBytes, backupPwd)
            assertTrue(restoreResult.isSuccess, "Restore should be successful even for backups from other devices")

            // Verify that the restored entity in the database is encrypted with Device B's key (which is the current getDatabaseSecretKey())
            val dbEntities = fakeDao.getAllPayslips().first()
            assertEquals(1, dbEntities.size)
            val restoredEntity = dbEntities.first()

            // Verify it decrypts successfully with current device key (Device B's key)
            val currentDeviceKey = CryptoHelper.getDatabaseSecretKey()
            val decryptedWithDeviceKey = CryptoHelper.decrypt(restoredEntity.ciphertext.hexToByteArray(), currentDeviceKey)
            assertTrue(decryptedWithDeviceKey.isSuccess, "Restored entity should be decryptable by the current device key")

            // And it should NOT decrypt with Device A's key anymore
            val deviceAKey = "SomeOtherDeviceKeyString12345678"
            val decryptedWithDeviceA = CryptoHelper.decrypt(restoredEntity.ciphertext.hexToByteArray(), deviceAKey)
            assertTrue(decryptedWithDeviceA.isFailure, "Restored entity should not be decryptable by Device A's key anymore")
        }
}
