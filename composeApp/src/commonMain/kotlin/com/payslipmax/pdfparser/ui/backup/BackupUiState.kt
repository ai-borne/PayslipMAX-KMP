package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.repository.RestoredRows
import com.payslipmax.pdfparser.repository.SkippedRows

/**
 * What the Backup & Restore sheet shows (docs/Plan/14a P7): the one state [BackupRestoreViewModel] owns. Counts, stages and codes only. No
 * state holds the password, a file's path or name, an exception or its text, so no `toString()` can carry them.
 */
sealed interface BackupUiState {
    /** Nothing is running. [lastBackupAt] is when this device last made a backup (epoch millis), or null if it never did. */
    data class Idle(
        val lastBackupAt: Long?,
    ) : BackupUiState

    /** A backup or restore is running. [total] is 0 while it is not known yet. A second Back Up or Restore is ignored. */
    data class Working(
        val op: BackupOp,
        val stage: BackupStage,
        val done: Int,
        val total: Int,
    ) : BackupUiState

    /** A picked backup was opened and checked, and nothing is written yet: the officer chooses Replace or Merge, or cancels. */
    data class Preview(
        val preview: RestorePreview,
    ) : BackupUiState

    data class Done(
        val summary: BackupSummary,
    ) : BackupUiState

    /** The failure's code only, never the exception: [of] reads its type through the one mapper ([BackupFailure.of]). */
    data class Failed(
        val failure: BackupFailure,
    ) : BackupUiState {
        companion object {
            fun of(error: Throwable?): Failed = Failed(BackupFailure.of(error))
        }
    }
}

/** What a finished operation reports: counts only, never amounts or months. */
sealed interface BackupSummary {
    /** A backup that read back and was handed to the share sheet, with what the device held when Back Up was pressed. */
    data class BackedUp(
        val payslips: Int,
        val part2Items: Int,
    ) : BackupSummary

    /** A restore that was committed: what it wrote and the rows it left out, per kind. */
    data class Restored(
        val restored: RestoredRows,
        val skipped: SkippedRows,
    ) : BackupSummary
}
