package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings

const val RestoreConfirmButtonTag = "restore_confirm_button"
const val RestoreCancelButtonTag = "restore_cancel_button"

/**
 * The officer's answer to a preview (D10: it always shows, on an empty device too). A device that holds something asks Replace or Merge; an
 * empty one has nothing to merge with, so one Restore button (Replace) does it. Replace with skipped rows asks a second time (D11).
 * [contents] is null until the device has been counted.
 */
@Composable
internal fun RestoreChoice(
    preview: RestorePreview,
    contents: BackupContents?,
    onCommit: (RestoreMode) -> Unit,
    onCancel: () -> Unit,
) {
    var askAgain by remember(preview) { mutableStateOf(false) }
    val replace = { if (replaceNeedsSecondConfirm(preview, RestoreMode.REPLACE)) askAgain = true else onCommit(RestoreMode.REPLACE) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        if (contents != null && restoreNeedsChoice(contents)) {
            Text(restoreChoiceTitle(contents), style = MaterialTheme.typography.titleSmall)
            Text(AppStrings.settingsRestoreExistingMsg, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
                Button(onClick = replace) { Text(AppStrings.settingsRestoreReplaceBtn) }
                OutlinedButton(onClick = { onCommit(RestoreMode.MERGE) }) { Text(AppStrings.settingsRestoreMergeBtn) }
            }
        } else if (contents != null) {
            Button(onClick = replace, modifier = Modifier.testTag(RestoreConfirmButtonTag)) { Text(BackupRestoreStrings.restoreBtn) }
        }
        TextButton(onClick = onCancel, modifier = Modifier.testTag(RestoreCancelButtonTag)) { Text(BackupRestoreStrings.cancelBtn) }
    }
    if (askAgain) {
        ReplaceSkipsDialog(preview.skipped, onConfirm = {
            askAgain = false
            onCommit(RestoreMode.REPLACE)
        }, onBack = { askAgain = false })
    }
}

/** D11: the second, explicit confirm for a Replace that leaves rows out; it names what will be lost. */
@Composable
private fun ReplaceSkipsDialog(
    skipped: SkippedRows,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onBack,
        title = { Text(BackupRestoreStrings.replaceSkipsTitle) },
        text = { Text(BackupRestoreStrings.replaceSkipsMessage(skipped)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(BackupRestoreStrings.replaceAnywayBtn) } },
        dismissButton = { TextButton(onClick = onBack) { Text(BackupRestoreStrings.goBackBtn) } },
    )
}
