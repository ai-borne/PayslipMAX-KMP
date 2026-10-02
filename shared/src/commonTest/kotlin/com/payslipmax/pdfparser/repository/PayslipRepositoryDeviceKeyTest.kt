package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.hexToByteArray
import com.payslipmax.pdfparser.database.toCorrectionEntity
import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
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
    fun testLegacyDecryptionFallbackSucceeds() =
        runTest {
            val payslip = createMockPayslip("08/2026")
            val legacyKey = CryptoHelper.getLegacyFallbackKey()
            val entity = payslip.toEncryptedEntity(legacyKey)

            val decrypted = entity.toDomain()
            assertEquals(payslip.dateStr, decrypted.dateStr)
        }

    @Test
    fun testUndecryptablePayslipIsSkippedNotWiped() =
        runTest {
            // Regression guard: getAllPayslips() used to wipe the *entire* table
            // (repository.clearAll()) the moment a single row failed to decrypt - e.g. a row
            // written by a different device's Keystore key after a whole-file "Local Restore".
            // That silently destroyed every other real payslip alongside the one bad row. The
            // undecryptable row must now be skipped from the returned list, and the underlying
            // data must be left untouched so it isn't lost.
            val fakeDao = FakePayslipDao()
            val fakeParser = FakePdfParser()
            val repository = PayslipRepository(fakeDao, fakeParser, kotlinx.coroutines.Dispatchers.Unconfined)

            val payslip = createMockPayslip("08/2026")
            val entity = payslip.toEncryptedEntity("TotallyWrongRandomPassword")
            fakeDao.insertPayslip(entity)

            val list = repository.getAllPayslips().first()
            assertTrue(list.isEmpty(), "The undecryptable row should be skipped from the visible list")
            assertEquals(
                1,
                fakeDao.getAllPayslips().first().size,
                "The undecryptable row must NOT be deleted - it may become readable later (e.g. after a key fix) and must not take other payslips down with it",
            )
        }

    @Test
    fun testBatchDecryptionReusesKeyWithoutBreakingFallback() =
        runTest {
            val fakeDao = FakePayslipDao()
            val fakeParser = FakePdfParser()
            val repository = PayslipRepository(fakeDao, fakeParser, kotlinx.coroutines.Dispatchers.Unconfined)

            val deviceKey = CryptoHelper.getDatabaseSecretKey()
            val legacyKey = CryptoHelper.getLegacyFallbackKey()

            val normalPayslip = createMockPayslip("08/2026")
            fakeDao.insertPayslip(normalPayslip.toEncryptedEntity(deviceKey))

            val legacyPayslip = createMockPayslip("07/2026")
            fakeDao.insertPayslip(legacyPayslip.toEncryptedEntity(legacyKey))
            val legacyCorrection = mapOf("basicPay" to 555.0).toCorrectionEntity("07/2026", legacyKey)
            fakeDao.insertCorrection(legacyCorrection)

            val corruptEntity =
                EncryptedPayslipEntity(
                    dateStr = "06/2026",
                    year = 2026,
                    monthNum = 6,
                    monthName = "June",
                    ciphertext = "badcafebabe",
                )
            fakeDao.insertPayslip(corruptEntity)

            val result = repository.getAllPayslips().first()

            assertEquals(2, result.size)
            val byDate = result.associateBy { it.dateStr }

            assertTrue(byDate.containsKey("08/2026"))
            assertTrue(byDate.containsKey("07/2026"))
            assertEquals(555.0, byDate["07/2026"]?.earnings?.basicPay)
        }
}
