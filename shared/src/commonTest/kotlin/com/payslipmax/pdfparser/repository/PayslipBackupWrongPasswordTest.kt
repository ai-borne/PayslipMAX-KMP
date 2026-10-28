package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.part2.data.BackupDamagedException
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a wrong password or a file that is not a PayslipMax backup is the commonest way a restore fails, and the officer saw the crypto
 * library's own text. The service must say so by type (the screen decides by type, never by text), must change nothing on the device, and
 * must not call an unreadable row inside an opened file a wrong password: the password was right. Synthetic data only.
 */
class PayslipBackupWrongPasswordTest {
    private val dao = FakePayslipDao()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined)

    /** Everything the device holds, as plain values, so "unchanged" means every table and not only the payslips. */
    private suspend fun snapshot(): List<Any?> =
        listOf(
            dao.getAllPayslips().first().map { it.dateStr to it.ciphertext },
            dao.getAllPdfs().map { it.dateStr to it.pdfData.toList() },
            dao.getAllCorrections().first().map { it.dateStr },
            dao.getAllGuideNotes().first().map { it.cardId },
            dao.getAllLedgerRecords().first().map { it.dateStr },
            dao.getAllFinancialInsights().first().map { it.id },
            dao.getAllRepresentationDrafts().first().map { it.id },
            dao.getAllDismissedDrafts().map { it.disputeMonth to it.disputeType },
            dao.getSettings().toString(),
        )

    private suspend fun assertRefusedAndUntouched(
        bytes: ByteArray,
        password: String,
        mode: RestoreMode,
    ) {
        val before = snapshot()

        val error = service.restore(bytes, password, mode).exceptionOrNull()

        assertTrue(error is DecryptionFailedException, "expected the typed refusal but was $error")
        assertNull(error.cause)
        assertEquals(before, snapshot(), "a refused restore must change nothing ($mode)")
    }

    @Test
    fun aRealBackupWithTheWrongPasswordIsRefusedByTypeAndChangesNothing() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            for (mode in RestoreMode.entries) assertRefusedAndUntouched(backupOf("08/2024"), "Not-The-Password#9", mode)
        }

    @Test
    fun aFileThatIsNotABackupIsRefusedTheSameWay() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            val aDocumentOfAnotherKind = ByteArray(300_000) { (it * 131 + 17).toByte() }
            for (mode in RestoreMode.entries) assertRefusedAndUntouched(aDocumentOfAnotherKind, BACKUP_PASSWORD, mode)
        }

    @Test
    fun anEmptyOrTinyFileIsRefusedTheSameWay() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            for (size in listOf(0, 1, 27, 44)) assertRefusedAndUntouched(ByteArray(size) { 7 }, BACKUP_PASSWORD, RestoreMode.REPLACE)
        }

    @Test
    fun aRowThatCannotBeReadInsideAFileThatOpenedIsADamagedBackupNotAWrongPassword() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            val before = snapshot()
            // The outer layer opens with the right password, but this payslip was sealed under another one and under no legacy key.
            val foreignRow = createMockPayslip("08/2024").toEncryptedEntity("Another-Password#3")
            val bytes = backupBytes(PortableBackup(PortableBackup.CURRENT_VERSION, listOf(foreignRow), emptyList(), null))

            val error = service.restore(bytes, BACKUP_PASSWORD, RestoreMode.REPLACE).exceptionOrNull()

            assertTrue(error is BackupDamagedException, "the password was right, so this is damage, but it was $error")
            assertEquals(before, snapshot())
        }

    @Test
    fun theRightPasswordStillRestores() =
        runTest {
            service.restore(backupOf("08/2024"), BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
        }

    @Test
    fun aVersionRefusalKeepsItsOwnType() =
        runTest {
            val newer = PortableBackup(PortableBackup.CURRENT_VERSION + 1, emptyList(), emptyList(), null)
            val json = Json.encodeToString(PortableBackup.serializer(), newer)
            val sealed = CryptoHelper.encrypt(json.encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()

            assertTrue(service.restore(sealed, BACKUP_PASSWORD).exceptionOrNull() is UnsupportedBackupVersionException)
        }
}
