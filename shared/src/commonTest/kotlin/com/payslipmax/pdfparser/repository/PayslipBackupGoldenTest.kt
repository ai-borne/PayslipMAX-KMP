package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.database.DismissedDraftEntity
import com.payslipmax.pdfparser.database.hexToByteArray
import com.payslipmax.pdfparser.database.toCorrectionList
import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.domain.FieldSource
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Backups are the only copy of an officer's data if a phone is lost, and they outlive app versions. These
 * tests restore archives that were written once and committed (`golden-backups/`, see [GoldenBackupCatalog]), so a change that
 * drifts from the real on-disk format fails here even if a backup built by today's test code would still pass. [GoldenBackupRestoreTest]
 * checks every golden's counts; these check the content of the oldest three.
 */
class PayslipBackupGoldenTest {
    private val dao = FakePayslipDao()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined)

    private suspend fun restore(bytes: ByteArray) = service.restore(bytes, Secret.of(BACKUP_PASSWORD), RestoreMode.REPLACE).getOrThrow()

    @Test
    fun theCommittedVersion1ArchiveStillRestores() =
        runTest {
            restore(GoldenBackupCatalog.bytes("jvm-legacy-v1.pcda"))

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertTrue(dao.getAllRepresentationDrafts().first().isEmpty())
        }

    @Test
    fun theCommittedVersion2ArchiveStillRestores() =
        runTest {
            restore(GoldenBackupCatalog.bytes("jvm-legacy-v2.pcda"))

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals(listOf("08/2024"), dao.getAllPdfs().map { it.dateStr })
            assertTrue(dao.getAllCorrections().first().isEmpty())
        }

    @Test
    fun theCommittedVersion3ArchiveRestoresEverythingItCarries() =
        runTest {
            restore(GoldenBackupCatalog.bytes("jvm-legacy-v3.pcda"))

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals("Golden body", dao.getAllRepresentationDrafts().first().single().bodyText)
            assertEquals(listOf(DismissedDraftEntity("08/2024", "SALARY_DROP")), dao.getAllDismissedDrafts())
            val correction = dao.getCorrectionByDate("08/2024")!!.toCorrectionList(CryptoHelper.getDatabaseSecretKey()).single()
            assertEquals(61000.0, correction.amount)
        }

    /**
     * WHY (docs/Plan/14a P5 item 8, P9): `fieldSource` is written into every payslip in every backup, and a shipped app reads it strictly.
     * `FieldSourceWireTest` pins its encoding with literals; this pins it in a file that was really written, so a refactor of the
     * serializer that changes a byte, or drops a value, fails here on both platforms (the iOS file is read by the JVM and the other way round).
     */
    @Test
    fun theFieldSourceGoldensCarryBothValuesExactlyAsWritten() =
        runTest {
            for (file in listOf("jvm-fieldsource-v6.pcda", "ios-fieldsource-v6.pcda")) {
                val bytes = GoldenBackupCatalog.bytes(file)
                val opened = openBackup(bytes)
                val texts = opened.backup.encryptedPayslips.map { opened.key.open(it.ciphertext.hexToByteArray()).getOrThrow().decodeToString() }
                assertEquals(1, texts.count { """"fieldSource":{"basicPay":"GEOMETRY","incomeTax":"GEMMA_FALLBACK"}""" in it }, "$file: the bytes of the map")

                restore(bytes)
                val map = dao.getAllPayslips().first().map { it.toDomain().fieldSource }.single { it.isNotEmpty() }
                assertEquals(mapOf("basicPay" to FieldSource.GEOMETRY, "incomeTax" to FieldSource.GEMMA_FALLBACK), map, file)
            }
        }
}
