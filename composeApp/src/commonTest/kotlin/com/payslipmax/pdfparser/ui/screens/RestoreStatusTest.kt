package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.RestoreReport
import com.payslipmax.pdfparser.repository.UnsupportedBackupVersionException
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a wrong password or a file that is not a backup is the commonest restore failure, and the officer used to read the crypto library's own
 * text ("error:1e000065:...BAD_DECRYPT"). The line is chosen by the failure's type and never by its text, so no library, file or row text can
 * reach the screen, and a restore that worked must read exactly as it always did.
 */
class RestoreStatusTest {
    @Test
    fun aCleanRestoreReadsAsItAlwaysDid() {
        val status = restoreStatus(Result.success(RestoreReport(0)))
        assertEquals(AppStrings.statusRestoreComplete, status.message)
        assertTrue(status.isSuccess)
    }

    @Test
    fun aRestoreThatLeftRowsOutSaysHowManyAndIsNotShownAsAPlainSuccess() {
        val status = restoreStatus(Result.success(RestoreReport(3)))
        assertEquals("${AppStrings.statusRestoreComplete} ${OrdersBankStrings.restoreSkipped(3)}", status.message)
        assertFalse(status.isSuccess, "lost rows must be noticed, so the line takes the warning colour")
        assertEquals("1 item could not be restored.", OrdersBankStrings.restoreSkipped(1))
    }

    @Test
    fun aWrongPasswordOrWrongFileSaysSoInOurOwnWords() {
        val status = restoreStatus(Result.failure(DecryptionFailedException()))
        assertEquals(BackupRestoreStrings.wrongPasswordOrNotABackup, status.message)
        assertFalse(status.isSuccess)
    }

    @Test
    fun aDamagedArchiveAndANewerVersionKeepTheirOwnFixedMessages() {
        val damaged = BackupDamagedException()
        assertEquals("${AppStrings.statusRestoreFailed}${damaged.message}", restoreStatus(Result.failure(damaged)).message)
        val newer = UnsupportedBackupVersionException(99)
        assertEquals("${AppStrings.statusRestoreFailed}${newer.message}", restoreStatus(Result.failure(newer)).message)
    }

    /** WHY: this replaces behaviour that showed any exception's message. The reason is chosen by type, never by text. */
    @Test
    fun noOtherFailureShowsItsOwnTextWhateverItHolds() {
        val leaky =
            listOf(
                IllegalStateException("error:1e000065:Cipher functions:OPENSSL_internal:BAD_DECRYPT"),
                IllegalArgumentException("Decrypt status: -4304 for /data/user/0/in.aiborne.payslipmax/files/backup-AB123.pcda"),
                Exception("Row for Officer Sample Name, net pay 98765.43"),
                IllegalStateException("wrong password"),
            )
        for (error in leaky) {
            val status = restoreStatus(Result.failure(error))
            assertEquals(BackupRestoreStrings.restoreFailedGeneric, status.message, "for ${error::class.simpleName}")
            assertFalse(status.isSuccess)
        }
    }

    @Test
    fun aFailureWithNoReasonAtAllIsTheGenericLine() {
        assertEquals(BackupRestoreStrings.restoreFailedGeneric, restoreFailedStatus(null).message)
    }

    @Test
    fun aFailureIsNeverMistakenForTheWrongPasswordByItsWording() {
        val status = restoreStatus(Result.failure(IllegalStateException("Wrong password, or this isn't a PayslipMax backup")))
        assertEquals(BackupRestoreStrings.restoreFailedGeneric, status.message)
    }
}
