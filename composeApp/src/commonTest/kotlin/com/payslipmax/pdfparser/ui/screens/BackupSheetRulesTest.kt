package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.ui.backup.BackupSummary
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P8, D11, D12): three rules decide whether the officer can lose data or be locked into a mistake, so each is one
 * pure function the sheet reads and nothing else decides:
 * - the sheet may not be closed while a backup or restore is running (D12: no Cancel, progress only);
 * - Back Up needs the password typed twice the same, because a typo is a backup that can never be opened;
 * - Replace with rows the backup could not read deletes what the device holds and restores less, so it asks a second time (D11).
 */
class BackupSheetRulesTest {
    private fun preview(skipped: SkippedRows) = RestorePreview(6, 1L, 3, 3, 0, 0, 0, 0, 2, 1, 5, skipped)

    @Test
    fun onlyAWorkingSheetRefusesToClose() {
        assertFalse(BackupUiState.Working(BackupOp.BACKUP, BackupStage.READING, 1, 2).sheetMayHide())
        for (state in listOf(
            BackupUiState.Idle(null),
            BackupUiState.Preview(preview(SkippedRows.NONE)),
            BackupUiState.Done(BackupSummary.Restored(SkippedRows.NONE)),
            BackupUiState.Failed(BackupFailure.DAMAGED),
        )) {
            assertTrue(state.sheetMayHide(), "$state")
        }
    }

    @Test
    fun theFormIsLiveExceptWhileWorking() {
        assertFalse(BackupUiState.Working(BackupOp.RESTORE, BackupStage.OPENING, 0, 0).formIsLive())
        assertTrue(BackupUiState.Idle(null).formIsLive())
        assertTrue(BackupUiState.Failed(BackupFailure.UNKNOWN).formIsLive())
    }

    @Test
    fun backUpNeedsTheSamePasswordTwice() {
        assertTrue(passwordsMatch("pass 1!", "pass 1!"))
        assertFalse(passwordsMatch("pass 1!", "pass 2!"))
        assertFalse(passwordsMatch("pass", ""), "a confirm left empty is a mismatch, not a pass")
        assertFalse(passwordsMatch("pass", "Pass"), "case counts")
    }

    @Test
    fun onlyReplaceWithSkippedRowsAsksASecondTime() {
        val skips = preview(SkippedRows(payslips = 2))
        assertTrue(replaceNeedsSecondConfirm(skips, RestoreMode.REPLACE))
        assertFalse(replaceNeedsSecondConfirm(skips, RestoreMode.MERGE), "Merge deletes nothing")
        assertFalse(replaceNeedsSecondConfirm(preview(SkippedRows.NONE), RestoreMode.REPLACE), "nothing is lost")
        assertTrue(replaceNeedsSecondConfirm(preview(SkippedRows(part2Pages = 1)), RestoreMode.REPLACE), "any kind counts")
    }
}
