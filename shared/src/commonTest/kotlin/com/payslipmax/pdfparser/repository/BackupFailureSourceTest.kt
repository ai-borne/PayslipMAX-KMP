package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P6, P10 "From P2" item 2): each refusal is thrown as the type its code is read from, with a fixed message, at the
 * place it is found: a file too large to be read is not "damaged", and a document whose JSON will not decode must not carry kotlinx's
 * message (which quotes the JSON, payslip values included) out of the service in its `Result`.
 */
class BackupFailureSourceTest {
    private val dao = FakePayslipDao()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined)

    /** A file of [size] bytes that is never held in memory: only the few bytes a reader asks for are made. */
    private class SizedSource(
        override val size: Long,
    ) : BackupSource {
        var largestRead = 0

        override fun read(
            offset: Long,
            length: Int,
        ): ByteArray = ByteArray(length) { 7 }.also { largestRead = maxOf(largestRead, length) }
    }

    @Test
    fun aFileWithoutAPageArchiveLargerThanAnyBackupWithoutPagesIsTooLargeAndIsNotRead() =
        runTest {
            val huge = SizedSource(128L * 1024 * 1024 + 1)

            val error = service.prepare(huge, BACKUP_PASSWORD).exceptionOrNull()

            assertIs<BackupTooLargeException>(error)
            assertTrue(huge.largestRead <= 64, "only the header and the archive marks were read, ${huge.largestRead} bytes")
        }

    @Test
    fun aDocumentThatOpensButIsNotABackupsJsonIsDamagedAndCarriesNoneOfIt() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            val before = dao.getAllPayslips().first().map { it.dateStr }
            val json = """{"version":2,"encryptedPayslips":[{"dateStr":"ABCDE1234F","year":"net 98765.43"""
            val file = CryptoHelper.encrypt(json.encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()

            val error = service.restore(file, BACKUP_PASSWORD).exceptionOrNull()

            assertIs<BackupDamagedException>(error)
            assertNull(error.cause)
            for (piece in listOf("ABCDE1234F", "98765", "JSON", "offset")) assertTrue(piece !in error.message.orEmpty(), piece)
            assertEquals(before, dao.getAllPayslips().first().map { it.dateStr })
        }
}
