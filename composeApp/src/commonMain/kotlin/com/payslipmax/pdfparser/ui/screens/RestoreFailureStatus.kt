package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings

/**
 * The line the sheet shows for a failed backup or restore, or null in any other state (docs/Plan/14a P7). It is the one fixed line of the
 * failure's code, so no exception, path or row text can reach the screen (F7). A finished operation is drawn by [BackupResultCard].
 */
internal fun BackupUiState.status(): BackupStatus? = (this as? BackupUiState.Failed)?.let { failedStatus(it.failure) }

/**
 * Whether the sheet opens by itself when Settings is shown again (after the app lock, say): on anything but Idle, because the officer has
 * not seen it yet: work still running, a preview waiting for Replace or Merge (it holds the picked file), or a result.
 */
internal fun BackupUiState.opensTheSheet(): Boolean = this !is BackupUiState.Idle

/** The status for a failed backup or restore: the one fixed line of its [BackupFailure] (docs/Plan/14a P6). */
internal fun failedStatus(failure: BackupFailure): BackupStatus = BackupStatus(BackupRestoreStrings.failure(failure), isSuccess = false)
