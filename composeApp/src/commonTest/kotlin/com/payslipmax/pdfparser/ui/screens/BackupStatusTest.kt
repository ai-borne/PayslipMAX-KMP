package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.BackupFileException
import com.payslipmax.pdfparser.repository.BackupStorageFullException
import com.payslipmax.pdfparser.repository.BackupVerifyException
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (F7, docs/Plan/14a P6): a failed backup used to show "Backup Failed: " followed by the exception's own message, which can carry the
 * file's path. The backup path now shows the same fixed line per failure type as a restore, and a backup that worked reads as it always did.
 */
class BackupStatusTest {
    private val contents = BackupContents(payslips = 3, part2Items = 0)

    @Test
    fun aBackupThatWorkedReadsAsItAlwaysDid() {
        val status = backupStatus(Result.success(Unit), contents)
        assertEquals(backupDoneStatus(contents), status)
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
            val status = backupStatus(Result.failure(error), contents)
            assertEquals(BackupRestoreStrings.failedGeneric, status.message)
            assertFalse(status.isSuccess)
            for (piece in listOf("/data", "ENOSPC", "ABCDE1234F", "98765", "BAD_DECRYPT", ".pcda")) assertFalse(piece in status.message, piece)
        }
    }

    @Test
    fun theBackupFailuresShowTheirOwnLines() {
        assertEquals(BackupRestoreStrings.verifyFailed, backupStatus(Result.failure(BackupVerifyException()), contents).message)
        assertEquals(BackupRestoreStrings.writeFailed, backupStatus(Result.failure(BackupFileException()), contents).message)
        assertEquals(BackupRestoreStrings.storageFull, backupStatus(Result.failure(BackupStorageFullException()), contents).message)
    }
}
