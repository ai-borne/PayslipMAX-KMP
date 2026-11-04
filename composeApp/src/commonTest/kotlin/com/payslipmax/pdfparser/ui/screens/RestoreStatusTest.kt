package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.BackupFileException
import com.payslipmax.pdfparser.repository.BackupStorageFullException
import com.payslipmax.pdfparser.repository.BackupTooLargeException
import com.payslipmax.pdfparser.repository.BackupVerifyException
import com.payslipmax.pdfparser.repository.NotABackupException
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.repository.UnsupportedBackupVersionException
import com.payslipmax.pdfparser.ui.backup.BackupSummary
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WHY: a wrong password or a file that is not a backup is the commonest restore failure, and the officer used to read the crypto library's own
 * text ("error:1e000065:...BAD_DECRYPT"). Since docs/Plan/14a P6 every failure shows the one fixed line of its `BackupFailure`, chosen by its
 * type and never by its text, so no library, file or row text can reach the screen, and a restore that worked must read exactly as it always did.
 * Rewritten in P7: the screen now reads the view model's state, so each case goes from the exception through `BackupUiState.Failed.of` (the
 * one step the view model takes) to the line the sheet shows; the intent is unchanged.
 */
class RestoreStatusTest {
    private fun restored(skipped: SkippedRows) = assertNotNull(BackupUiState.Done(BackupSummary.Restored(skipped)).status())

    private fun failed(error: Throwable?) = assertNotNull(BackupUiState.Failed.of(error).status())

    /** Messages a real failure can carry: a path, a PAN-like id, the crypto library's words, kotlinx quoting a payslip. */
    private val leaky =
        listOf(
            "Decrypt status: -4304 for /data/user/0/in.aiborne.payslipmax/files/backup-AB123.pcda",
            "Row for ABCDE1234F, net pay 98765.43",
            "error:1e000065:Cipher functions:OPENSSL_internal:BAD_DECRYPT",
            "Unexpected JSON token at offset 40: {\"dateStr\":\"03/2024\",\"basicPay\":98765.43}",
        )

    @Test
    fun aCleanRestoreReadsAsItAlwaysDid() {
        val status = restored(SkippedRows.NONE)
        assertEquals(AppStrings.statusRestoreComplete, status.message)
        assertTrue(status.isSuccess)
    }

    @Test
    fun aRestoreThatLeftRowsOutSaysHowManyAndIsNotShownAsAPlainSuccess() {
        // One line counts every kind: a payslip left out is as lost as a page.
        val status = restored(SkippedRows(payslips = 1, part2Pages = 2))
        assertEquals("${AppStrings.statusRestoreComplete} ${OrdersBankStrings.restoreSkipped(3)}", status.message)
        assertFalse(status.isSuccess, "lost rows must be noticed, so the line takes the warning colour")
        assertEquals("1 item could not be restored.", OrdersBankStrings.restoreSkipped(1))
    }

    @Test
    fun aWrongPasswordOrWrongFileSaysSoInOurOwnWords() {
        val status = failed(DecryptionFailedException())
        assertEquals(BackupRestoreStrings.wrongPasswordOrNotABackup, status.message)
        assertFalse(status.isSuccess)
    }

    /** Rewritten in P6: "damaged and newer keep their own fixed messages" showed the exception's message; now each type has its copy line. */
    @Test
    fun everyFailureShowsTheFixedLineOfItsTypeNeverTheExceptionsMessage() {
        val expected =
            listOf(
                NotABackupException() to BackupRestoreStrings.notABackup,
                UnsupportedBackupVersionException(99) to BackupRestoreStrings.newerVersion,
                BackupDamagedException() to BackupRestoreStrings.damaged,
                SerializationException("bad") to BackupRestoreStrings.damaged,
                BackupTooLargeException() to BackupRestoreStrings.tooLarge,
                BackupStorageFullException() to BackupRestoreStrings.storageFull,
                BackupVerifyException() to BackupRestoreStrings.verifyFailed,
                BackupFileException() to BackupRestoreStrings.writeFailed,
            )
        for ((error, line) in expected) {
            val status = failed(error)
            assertEquals(line, status.message, "for ${error::class.simpleName}")
            assertFalse(status.isSuccess)
        }
    }

    /** WHY: this replaces behaviour that showed any exception's message. The reason is chosen by type, never by text, wrapped or not. */
    @Test
    fun noFailureShowsItsOwnTextWhateverItHolds() {
        for (text in leaky) {
            val plain = failed(IllegalStateException(text)).message
            assertEquals(BackupRestoreStrings.failedGeneric, plain)
            val wrapped = failed(RuntimeException(text, BackupDamagedException())).message
            assertEquals(BackupRestoreStrings.damaged, wrapped, "a wrapped failure is found by its type")
            for (piece in listOf("ABCDE1234F", "98765", "BAD_DECRYPT", "/data", "03/2024", "JSON")) {
                assertFalse(piece in plain || piece in wrapped, "'$piece' reached the screen")
            }
        }
    }

    @Test
    fun aFailureWithNoReasonAtAllIsTheGenericLine() {
        assertEquals(BackupRestoreStrings.failedGeneric, failed(null).message)
    }

    @Test
    fun aFailureIsNeverMistakenForTheWrongPasswordByItsWording() {
        val status = failed(IllegalStateException("Wrong password, or this isn't a PayslipMax backup"))
        assertEquals(BackupRestoreStrings.failedGeneric, status.message)
    }
}
