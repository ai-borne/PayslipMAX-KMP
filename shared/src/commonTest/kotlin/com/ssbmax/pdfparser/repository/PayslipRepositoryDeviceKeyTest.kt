package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.crypto.CryptoHelper
import com.ssbmax.pdfparser.crypto.getLegacyFallbackKey
import com.ssbmax.pdfparser.database.hexToByteArray
import com.ssbmax.pdfparser.database.toDomain
import com.ssbmax.pdfparser.database.toEncryptedEntity
import com.ssbmax.pdfparser.domain.Deductions
import com.ssbmax.pdfparser.domain.Earnings
import com.ssbmax.pdfparser.domain.LedgerBalances
import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.domain.PayslipSummary
import com.ssbmax.pdfparser.testing.FakePayslipDao
import com.ssbmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayslipRepositoryDeviceKeyTest {
    private fun createMockPayslip(dateStr: String): ParsedPayslip {
        val split = dateStr.split("/")
        val month = split[0].toInt()
        val year = split[1].toInt()
        return ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = year,
            monthNum = month,
            monthName = "Month_$month",
            dateStr = dateStr,
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
            deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(100.0, 80.0, 20.0),
            taxAndSavings = null,
        )
    }

    @Test
    fun testDefaultEncryptionDoesNotUseLegacyFallback() =
        runTest {
            val payslip = createMockPayslip("08/2026")

            // Encrypt with default parameters (should use device-specific key)
            val entity = payslip.toEncryptedEntity()
            val ciphertextBytes = entity.ciphertext.hexToByteArray()

            // 1. Verify decryption with the device key succeeds
            val deviceKey = CryptoHelper.getDatabaseSecretKey()
            val decryptDeviceResult = CryptoHelper.decrypt(ciphertextBytes, deviceKey)
            assertTrue(decryptDeviceResult.isSuccess, "Decryption with device key should succeed")

            // 2. Verify decryption with the legacy fallback fails
            val legacyKey = CryptoHelper.getLegacyFallbackKey()
            val decryptLegacyResult = CryptoHelper.decrypt(ciphertextBytes, legacyKey)
            assertTrue(decryptLegacyResult.isFailure, "Decryption with legacy fallback key must fail")
        }

    @Test
    fun testUniversalBackupEncryptsWithBackupPasswordAndImportsToDeviceKey() =
        runTest {
            val fakeDao = FakePayslipDao()
            val fakeParser = FakePdfParser()
            val repository = PayslipRepository(fakeDao, fakeParser, kotlinx.coroutines.Dispatchers.Unconfined)

            val payslip = createMockPayslip("08/2026")
            fakeParser.result = Result.success(payslip)

            // Import payslip (which encrypts it with current device key)
            repository.importPayslip(byteArrayOf(1, 2, 3), "pdf-password", "payslip.pdf")

            // Export backup using a backup password
            val backupPwd = "MyBackupPassword123!"
            val exportResult = repository.exportUniversalBackup(backupPwd)
            assertTrue(exportResult.isSuccess)
            val backupBytes = exportResult.getOrThrow()

            // Clear the DAO to simulate restore on a new/clean device
            repository.clearAll()
            assertTrue(repository.getAllPayslips().first().isEmpty())

            // Restore backup using the backup password
            val importResult = repository.importUniversalBackup(backupBytes, backupPwd)
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
            val restoreResult = repository.importUniversalBackup(encryptedBackupBytes, backupPwd)
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

    @Test
    fun testLegacyDecryptionFallbackSucceeds() =
        runTest {
            val payslip = createMockPayslip("08/2026")
            val legacyKey = CryptoHelper.getLegacyFallbackKey()
            val entity = payslip.toEncryptedEntity(legacyKey)

            val decrypted = entity.toDomain()
            assertEquals(payslip.dateStr, decrypted.dateStr)
        }

    @Test
    fun testGracefulRecoveryOnUndecryptableData() =
        runTest {
            val fakeDao = FakePayslipDao()
            val fakeParser = FakePdfParser()
            val repository = PayslipRepository(fakeDao, fakeParser, kotlinx.coroutines.Dispatchers.Unconfined)

            val payslip = createMockPayslip("08/2026")
            val entity = payslip.toEncryptedEntity("TotallyWrongRandomPassword")
            fakeDao.insertPayslip(entity)

            val list = repository.getAllPayslips().first()
            assertTrue(list.isEmpty(), "List should be empty due to graceful recovery")
            assertTrue(fakeDao.getAllPayslips().first().isEmpty(), "Database should have been cleared")
        }
}
