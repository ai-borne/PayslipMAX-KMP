package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.text.TextLayoutResult
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.ui.backup.BackupUiState

/** Shared fixtures for the sheet's compose UI tests (docs/Plan/14a P8): what the device holds, a preview, and the sheet body with recorded calls. */
internal val heldOnDevice = BackupContents(payslips = 5, part2Items = 2)
internal val skippedExample = SkippedRows(payslips = 2, part2Pages = 1)

internal fun previewOf(
    skipped: SkippedRows = SkippedRows.NONE,
    exportedAt: Long? = 1_762_128_000_000L,
) = RestorePreview(6, exportedAt, 48, 48, 0, 0, 0, 0, 3, 12, 120, skipped)

internal class SheetCalls {
    var backedUp: String? = null
    var restored: String? = null
    val commits = mutableListOf<RestoreMode>()
    var cancels = 0
}

@Composable
internal fun SheetUnderTest(
    state: BackupUiState,
    contents: BackupContents? = heldOnDevice,
    calls: SheetCalls = SheetCalls(),
) {
    BackupRestoreSheetBody(
        state = state,
        contents = contents,
        canBackup = true,
        actions =
            BackupSheetActions(
                onBackUp = {
                    calls.backedUp = it
                    null
                },
                onRestore = {
                    calls.restored = it
                    null
                },
                onCommit = { calls.commits += it },
                onCancel = { calls.cancels++ },
            ),
    )
}

internal fun SemanticsNodeInteraction.textColor(): Color {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
    return results.first().layoutInput.style.color
}
