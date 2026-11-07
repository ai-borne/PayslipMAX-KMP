package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.BackupFileException
import com.payslipmax.pdfparser.repository.BackupStorageFullException
import com.payslipmax.pdfparser.repository.BackupVerifyException
import com.payslipmax.pdfparser.ui.backup.BackupSummary
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (F7, docs/Plan/14a P6): a failed backup used to show "Backup Failed: " followed by the exception's own message, which can carry the
 * file's path. The backup path now shows the same fixed line per failure type as a restore, and a backup that worked reads as it always did.
 * Rewritten in P7: the screen reads the view model's state, so each failure goes through `BackupUiState.Failed.of`, as the view model's does.
 */
class BackupStatusTest {
    private val contents = BackupContents(payslips = 3, part2Items = 0)

    private fun failed(error: Throwable) = assertNotNull(BackupUiState.Failed.of(error).status())

    /** Rewritten in P8: a finished backup is drawn by `BackupResultCard` from `backupDoneStatus`; `status()` speaks for failures only. */
    @Test
    fun aBackupThatWorkedReadsAsItAlwaysDid() {
        assertNull(BackupUiState.Done(BackupSummary.BackedUp(contents.payslips, contents.part2Items)).status())
        val status = backupDoneStatus(contents)
        assertEquals("3 ${AppStrings.labelPayslipsBackedUp}", status.message)
        assertTrue(status.isSuccess)
    }

    @Test
    fun aFailedBackupNeverShowsTheExceptionsMessage() {
        val leaky =
            listOf(
                IllegalStateException("/data/user/0/in.aiborne.payslipmax/cache/PayslipMax-backup.pcda: open failed: ENOSPC"),
                RuntimeException("ABCDE1234F 98765.43 BAD_DECRYPT"),
            )
        for (error in leaky) {
            val status = failed(error)
            assertEquals(BackupRestoreStrings.failedGeneric, status.message)
            assertFalse(status.isSuccess)
            for (piece in listOf("/data", "ENOSPC", "ABCDE1234F", "98765", "BAD_DECRYPT", ".pcda")) assertFalse(piece in status.message, piece)
        }
    }

    @Test
    fun theBackupFailuresShowTheirOwnLines() {
        assertEquals(BackupRestoreStrings.verifyFailed, failed(BackupVerifyException()).message)
        assertEquals(BackupRestoreStrings.writeFailed, failed(BackupFileException()).message)
        assertEquals(BackupRestoreStrings.storageFull, failed(BackupStorageFullException()).message)
    }
}
