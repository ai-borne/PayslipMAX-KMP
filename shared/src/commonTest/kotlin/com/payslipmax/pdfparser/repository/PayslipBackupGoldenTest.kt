package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.DismissedDraftEntity
import com.payslipmax.pdfparser.database.toCorrectionList
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Backups are the only copy of an officer's data if a phone is lost, and they outlive app versions. These
 * tests restore archives that were written once and committed ([GoldenBackups]), so a change that drifts
 * from the real on-disk format fails here even if a backup built by today's test code would still pass.
 */
class PayslipBackupGoldenTest {
    private val dao = FakePayslipDao()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined)

    private suspend fun restore(bytes: ByteArray) = service.restore(bytes, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

    @Test
    fun theCommittedVersion1ArchiveStillRestores() =
        runTest {
            restore(GoldenBackups.v1LegacyKey)

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertTrue(dao.getAllRepresentationDrafts().first().isEmpty())
        }

    @Test
    fun theCommittedVersion2ArchiveStillRestores() =
        runTest {
            restore(GoldenBackups.v2)

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals(listOf("08/2024"), dao.getAllPdfs().map { it.dateStr })
            assertTrue(dao.getAllCorrections().first().isEmpty())
        }

    @Test
    fun theCommittedVersion3ArchiveRestoresEverythingItCarries() =
        runTest {
            restore(GoldenBackups.v3)

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals("Golden body", dao.getAllRepresentationDrafts().first().single().bodyText)
            assertEquals(listOf(DismissedDraftEntity("08/2024", "SALARY_DROP")), dao.getAllDismissedDrafts())
            val correction = dao.getCorrectionByDate("08/2024")!!.toCorrectionList(CryptoHelper.getDatabaseSecretKey()).single()
            assertEquals(61000.0, correction.amount)
        }
}
