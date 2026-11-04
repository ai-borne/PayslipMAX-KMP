package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.ui.backup.BackupSummary
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P7): the app lock takes Settings off the screen, and the sheet with it, while a backup or restore carries on in the
 * view model. When Settings comes back (after unlock, or when the officer returns to it) the sheet must open again on anything the officer
 * has not seen: the work still running, a preview waiting for Replace or Merge (it holds the picked file), or the result. Idle stays shut.
 */
class BackupSheetArrivalTest {
    private val preview = RestorePreview(6, null, 1, 1, 0, 0, 0, 0, 0, 0, 0, SkippedRows.NONE)

    @Test
    fun theSheetOpensOnArrivalOnEveryStateButIdle() {
        assertFalse(BackupUiState.Idle(null).opensTheSheet())
        for (state in listOf(
            BackupUiState.Working(BackupOp.RESTORE, BackupStage.READING, 1, 2),
            BackupUiState.Preview(preview),
            BackupUiState.Done(BackupSummary.Restored(SkippedRows.NONE)),
            BackupUiState.Failed(BackupFailure.DAMAGED),
        )) {
            assertTrue(state.opensTheSheet(), "$state")
        }
    }
}
