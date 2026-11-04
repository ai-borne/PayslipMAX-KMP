package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.ui.backup.BackupSummary
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/**
 * The line the sheet shows for a finished backup or restore, or null while nothing has finished (docs/Plan/14a P7). A backup that worked
 * reads as it always did; a failure is the one fixed line of its code, so no exception, path or row text can reach the screen (F7).
 */
internal fun BackupUiState.status(): BackupStatus? =
    when (this) {
        is BackupUiState.Done ->
            when (val summary = summary) {
                is BackupSummary.BackedUp -> backupDoneStatus(BackupContents(summary.payslips, summary.part2Items))
                is BackupSummary.Restored -> restoreStatus(summary.skipped)
            }
        is BackupUiState.Failed -> failedStatus(failure)
        is BackupUiState.Idle, is BackupUiState.Working, is BackupUiState.Preview -> null
    }

/**
 * Whether the sheet opens by itself when Settings is shown again (after the app lock, say): on anything but Idle, because the officer has
 * not seen it yet: work still running, a preview waiting for Replace or Merge (it holds the picked file), or a result.
 */
internal fun BackupUiState.opensTheSheet(): Boolean = this !is BackupUiState.Idle

/** A restore that left rows out says how many (every kind, one total), in the warning colour: those rows are lost, so the count must be noticed. */
internal fun restoreStatus(skipped: SkippedRows): BackupStatus {
    if (skipped.total == 0) return BackupStatus(AppStrings.statusRestoreComplete, isSuccess = true)
    return BackupStatus("${AppStrings.statusRestoreComplete} ${OrdersBankStrings.restoreSkipped(skipped.total)}", isSuccess = false)
}

/** The status for a failed backup or restore: the one fixed line of its [BackupFailure] (docs/Plan/14a P6). */
internal fun failedStatus(failure: BackupFailure): BackupStatus = BackupStatus(BackupRestoreStrings.failure(failure), isSuccess = false)
