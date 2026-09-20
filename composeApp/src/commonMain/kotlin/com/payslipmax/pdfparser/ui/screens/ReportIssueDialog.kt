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
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport

/** Keeps the pre-filled `mailto:` URL comfortably within what mail apps accept. */
private const val MAX_DESCRIPTION_LENGTH = 1000
private const val DESCRIPTION_MIN_LINES = 4

@Composable
fun ReportIssueDialog(
    onDismiss: () -> Unit,
    onSend: (description: String) -> Unit,
) {
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsSupport.reportIssueDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
                ReportIssuePrivacyNotice()
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(MAX_DESCRIPTION_LENGTH) },
                    label = { Text(AppStringsSupport.reportIssueDescriptionLabel) },
                    minLines = DESCRIPTION_MIN_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = description.isNotBlank(),
                onClick = {
                    onSend(description)
                    onDismiss()
                },
            ) { Text(AppStringsSupport.reportIssueSendBtn) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.btnCancel) }
        },
    )
}

@Composable
private fun ReportIssuePrivacyNotice() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(AppDimensions.SpacingSmall),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = AppStringsSupport.reportIssuePrivacyNotice,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(AppDimensions.SpacingSmall),
        )
    }
}
