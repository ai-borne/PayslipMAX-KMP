package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.part2.data.BackupDamagedException
import com.payslipmax.pdfparser.repository.RestoreReport
import com.payslipmax.pdfparser.repository.UnsupportedBackupVersionException
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a restore that quietly leaves rows out is lost data (plan Phase 11 item 10). The officer must be told how many, and a restore that
 * left nothing out must read exactly as it always did.
 */
class RestoreStatusTest {
    @Test
    fun aCleanRestoreReadsAsItAlwaysDid() {
        val status = restoreStatus(Result.success(RestoreReport(0)))
        assertEquals(AppStrings.statusRestoreComplete, status.message)
        assertTrue(status.isSuccess)
    }

    @Test
    fun aRestoreThatLeftRowsOutSaysHowManyAndIsNotShownAsPlainSuccess() {
        val status = restoreStatus(Result.success(RestoreReport(3)))
        assertEquals("${AppStrings.statusRestoreComplete} 3 items could not be restored.", status.message)
        assertFalse(status.isSuccess, "it shows in the warning colour so the count is noticed")
    }

    @Test
    fun oneItemIsNotPlural() {
        assertEquals("1 item could not be restored.", Part2VaultStrings.restoreSkipped(1))
        assertEquals("2 items could not be restored.", Part2VaultStrings.restoreSkipped(2))
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

    /** WHY: this test replaces one that pinned the reported fault (any exception's message shown). The reason is chosen by type, never by text. */
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
        // The classification is by type: an unrelated exception whose text says "wrong password" must not get the wrong-password line.
        val status = restoreStatus(Result.failure(IllegalStateException("Wrong password, or this isn't a PayslipMax backup")))
        assertEquals(BackupRestoreStrings.restoreFailedGeneric, status.message)
    }
}
