package com.payslipmax.pdfparser.ui.screens

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
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

private const val MESSAGE_MIN_LINES = 4

/** Bounds the field so a full-length text scrolls inside it; unbounded, the dialog outgrew the screen and hid its buttons. */
private const val MESSAGE_MAX_LINES = 8

/**
 * A short free-text message the user composes before their mail app opens: a privacy [notice], one multi-line field limited
 * to [maxLength] characters, and a confirm button that stays off until there is text. The text is held in memory only, so
 * Cancel, Back and a lost screen discard it. All copy is passed in; [onConfirm] gets the text as typed and the dialog then
 * dismisses itself. Used by Report an Issue and the Guide's Suggest a correction.
 */
@Composable
fun SupportMessageDialog(
    title: String,
    notice: String,
    fieldLabel: String,
    confirmLabel: String,
    maxLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (message: String) -> Unit,
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
                SupportMessageNotice(notice)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(maxLength) },
                    label = { Text(fieldLabel) },
                    minLines = MESSAGE_MIN_LINES,
                    maxLines = MESSAGE_MAX_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = message.isNotBlank(),
                onClick = {
                    onConfirm(message)
                    onDismiss()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.btnCancel) }
        },
    )
}

@Composable
private fun SupportMessageNotice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(AppDimensions.SpacingSmall),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(AppDimensions.SpacingSmall),
        )
    }
}
