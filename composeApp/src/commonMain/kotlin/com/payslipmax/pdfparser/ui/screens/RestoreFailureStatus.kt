package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.RestoreReport
import com.payslipmax.pdfparser.repository.UnsupportedBackupVersionException
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** A restore that left Part II rows out says how many, in the warning colour: those rows are lost, so the count must be noticed. */
internal fun restoreStatus(result: Result<RestoreReport>): BackupStatus {
    val report = result.getOrNull() ?: return restoreFailedStatus(result.exceptionOrNull())
    if (report.skipped == 0) return BackupStatus(AppStrings.statusRestoreComplete, isSuccess = true)
    return BackupStatus("${AppStrings.statusRestoreComplete} ${OrdersBankStrings.restoreSkipped(report.skipped)}", isSuccess = false)
}

/**
 * The status for a restore that failed, chosen by the failure's type and never by its text: a wrong password or file has its own line, a
 * damaged archive and a newer backup version keep the fixed message they were written with, and anything else gets one generic line, so no
 * library, file or row text can reach the screen.
 */
internal fun restoreFailedStatus(error: Throwable?): BackupStatus =
    when (error) {
        is DecryptionFailedException -> BackupStatus(BackupRestoreStrings.wrongPasswordOrNotABackup, isSuccess = false)
        is BackupDamagedException, is UnsupportedBackupVersionException ->
            BackupStatus("${AppStrings.statusRestoreFailed}${error.message}", isSuccess = false)
        else -> BackupStatus(BackupRestoreStrings.restoreFailedGeneric, isSuccess = false)
    }
