package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.UnsupportedBackupVersionException
import com.payslipmax.pdfparser.repository.seedDeviceMonth
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P3; F9, F10): 1.5.0 given a newer file showed "BAD_DECRYPT" instead of "update the app", because nothing in the file
 * said what it was before decrypting it, and the 10,000 PBKDF2 iterations could never be raised because no file recorded them. A v6 file
 * starts with a plaintext header that says both. These pin its bytes literally (the file is a contract with every later app), that a later
 * envelope, KDF or format answers "update the app" before any decrypt (shown with a WRONG password: a decrypt would say so), that numbers
 * outside their bounds are refused before the KDF runs (2^32 - 1 iterations would hang it), and that a header edited after writing is a
 * wrong-password failure, since every seal authenticates it. Every refusal leaves the device as it was.
 */
class BackupEnvelopeTest {
    private val dao = FakePayslipDao()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined)

    private fun be(
        value: Long,
        width: Int,
    ) = ByteArray(width) { (value ushr (8 * (width - 1 - it))).toByte() }

    /** A header by hand, from the layout in the plan, not from the production writer. */
    private fun header(
        envelope: Int = 1,
        kdf: Int = 1,
        iterations: Long = 600_000,
        format: Int = 6,
        saltLength: Int = 16,
    ) = "PCDX".encodeToByteArray() + byteArrayOf(envelope.toByte(), kdf.toByte()) + be(iterations, 4) + be(format.toLong(), 2) +
        byteArrayOf(saltLength.toByte()) + ByteArray(16) { (it + 1).toByte() }

    private suspend fun exported(): ByteArray {
        val source = FakePayslipDao()
        source.seedDeviceMonth("08/2024")
        val file = PayslipBackupService(source, Dispatchers.Unconfined).export(Secret.of(BACKUP_PASSWORD)).getOrThrow()
        assertContentEquals("PCDX".encodeToByteArray(), file.copyOf(4), "a new backup is a v6 file")
        return file
    }

    /** A real v6 file's page archive, without its header: a header put in front of it is the only thing that differs. */
    private suspend fun realBody(): ByteArray = exported().let { it.copyOfRange(29, it.size) }

    private suspend fun assertRefused(
        file: ByteArray,
        password: String,
        expected: KClass<out Exception>,
    ) {
        val before = dao.getAllPayslips().first().map { it.dateStr }

        val error = service.restore(file, Secret.of(password), RestoreMode.REPLACE).exceptionOrNull()

        assertTrue(error != null && expected.isInstance(error), "expected ${expected.simpleName} but was $error")
        assertEquals(before, dao.getAllPayslips().first().map { it.dateStr }, "a refused restore must change nothing")
    }

    @Test
    fun aNewBackupStartsWithThePlaintextHeaderThenThePageArchive() =
        runTest {
            val file = exported()

            assertContentEquals("PCDX".encodeToByteArray(), file.copyOfRange(0, 4), "magic")
            assertEquals(1, file[4].toInt(), "envelope version")
            assertEquals(1, file[5].toInt(), "KDF: PBKDF2-HMAC-SHA256")
            assertContentEquals(be(600_000, 4), file.copyOfRange(6, 10), "iterations (D6)")
            assertContentEquals(be(6, 2), file.copyOfRange(10, 12), "format version")
            assertEquals(16, file[12].toInt(), "salt length")
            assertContentEquals("PCDB".encodeToByteArray(), file.copyOfRange(29, 33), "the page archive follows, always (one shape)")
            assertFalse(file.copyOfRange(13, 29).contentEquals(exported().copyOfRange(13, 29)), "every backup has its own salt")
        }

    @Test
    fun aLaterEnvelopeFormatOrKdfAnswersUpdateTheAppBeforeAnythingIsDecrypted() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            val body = realBody()
            for (later in listOf(header(envelope = 2), header(format = 7), header(kdf = 2), header(envelope = 255, iterations = 0))) {
                assertRefused(later + body, "Not-The-Password#9", UnsupportedBackupVersionException::class)
            }
        }

    @Test
    fun numbersOutsideTheirBoundsAreADamagedFileRefusedBeforeTheKdfRuns() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            val outOfBounds =
                listOf(
                    header(iterations = 9_999),
                    header(iterations = 5_000_001),
                    header(iterations = 0xFFFF_FFFFL),
                    header(iterations = 0),
                    header(saltLength = 15),
                    header(saltLength = 17),
                )
            // A real page archive behind each header, so nothing but the bounds stands between the file and the KDF (and a decrypt error).
            val body = realBody()
            for (header in outOfBounds) assertRefused(header + body, BACKUP_PASSWORD, BackupDamagedException::class)
            for (cut in listOf(5, 20, 28)) assertRefused(header().copyOf(cut), BACKUP_PASSWORD, BackupDamagedException::class)
        }

    @Test
    fun aHeaderEditedAfterWritingIsAWrongPasswordFailureNotAnUnreadableRow() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            val good = exported()
            val salt = good.copyOf().also { it[20] = (it[20] + 1).toByte() }
            val iterations = good.copyOf().also { it[9] = (it[9] + 1).toByte() }
            // Format 5 is known, so only the header's authentication can catch this one: the key does not depend on it.
            val format = good.copyOf().also { it[11] = 5 }

            for (tampered in listOf(salt, iterations, format)) assertRefused(tampered, BACKUP_PASSWORD, DecryptionFailedException::class)
        }

    @Test
    fun theRightPasswordStillRestoresAV6File() =
        runTest {
            service.restore(exported(), Secret.of(BACKUP_PASSWORD), RestoreMode.REPLACE).getOrThrow()

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
        }
}
