package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.RestoreReport
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** A restore that left rows out says how many (every kind, one total), in the warning colour: those rows are lost, so the count must be noticed. */
internal fun restoreStatus(result: Result<RestoreReport>): BackupStatus {
    val report = result.getOrNull() ?: return failedStatus(result.exceptionOrNull())
    val skipped = report.skipped.total
    if (skipped == 0) return BackupStatus(AppStrings.statusRestoreComplete, isSuccess = true)
    return BackupStatus("${AppStrings.statusRestoreComplete} ${OrdersBankStrings.restoreSkipped(skipped)}", isSuccess = false)
}

/** A backup handed to the share sheet says what it holds; a failed one shows its fixed line, never the exception's message (F7). */
internal fun backupStatus(
    result: Result<Unit>,
    contents: BackupContents,
): BackupStatus = if (result.isSuccess) backupDoneStatus(contents) else failedStatus(result.exceptionOrNull())

/**
 * The status for a failed backup or restore: the one fixed line of its [BackupFailure], chosen by the failure's type (docs/Plan/14a P6),
 * so no library, file or row text can reach the screen.
 */
internal fun failedStatus(error: Throwable?): BackupStatus = BackupStatus(BackupRestoreStrings.failure(BackupFailure.of(error)), isSuccess = false)
