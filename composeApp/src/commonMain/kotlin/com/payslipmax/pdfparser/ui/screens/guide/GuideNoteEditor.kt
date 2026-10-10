package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.payslipmax.pdfparser.guide.domain.GUIDE_NOTE_MAX_CHARS
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideColors
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings

private const val NOTE_MIN_LINES = 4

/** Bounds the field so a full-length note scrolls inside it; unbounded, the dialog outgrew the screen and hid its buttons (M5). */
private const val NOTE_MAX_LINES = 8

/**
 * Where a note is typed or changed. The draft is plain `remember` state, never saved with the screen: Cancel, Back, a tab
 * switch, the app lock and process death all discard it (as for Suggest a correction). The field stops at the domain's
 * [GUIDE_NOTE_MAX_CHARS] and shows a counter; [GuideNote] enforces the same limit again when the note is stored. Save stays off for
 * blank text, because deleting a note is its own button. [failed] adds a retry line and keeps the draft.
 */
@Composable
internal fun GuideNoteEditorDialog(
    initial: String,
    failed: Boolean,
    onCancel: () -> Unit,
    onSave: (text: String) -> Unit,
) {
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val text = field.text
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(GuideMaintenanceStrings.noteEditorTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
                NoteNotice(GuideMaintenanceStrings.noteEditorNotice)
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = if (it.text.length <= GUIDE_NOTE_MAX_CHARS) it else TextFieldValue(it.text.take(GUIDE_NOTE_MAX_CHARS), TextRange(GUIDE_NOTE_MAX_CHARS)) },
                    label = { Text(GuideMaintenanceStrings.noteEditorLabel) },
                    supportingText = { Text(GuideMaintenanceStrings.noteCounter(text.length)) },
                    minLines = NOTE_MIN_LINES,
                    maxLines = NOTE_MAX_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (failed) Text(GuideMaintenanceStrings.noteFailed, style = MaterialTheme.typography.bodySmall, color = GuideColors.watchOut())
            }
        },
        confirmButton = { Button(enabled = text.isNotBlank(), onClick = { onSave(text) }) { Text(GuideMaintenanceStrings.noteSave) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(AppStrings.btnCancel) } },
    )
}

/** Asks before a note is deleted: it exists only on this device, so there is no way back. */
@Composable
internal fun GuideNoteDeleteDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(GuideMaintenanceStrings.noteDeleteTitle) },
        text = { Text(GuideMaintenanceStrings.noteDeleteBody) },
        confirmButton = { Button(onClick = onConfirm) { Text(GuideMaintenanceStrings.noteDeleteConfirm) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(AppStrings.btnCancel) } },
    )
}

@Composable
private fun NoteNotice(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(AppDimensions.SpacingSmall), modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(AppDimensions.SpacingSmall))
    }
}
