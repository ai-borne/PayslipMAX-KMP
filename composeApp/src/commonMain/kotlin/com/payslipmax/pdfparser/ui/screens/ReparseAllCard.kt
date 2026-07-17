@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import com.payslipmax.pdfparser.repository.ReparseSummary
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

const val ReparsePasswordFieldTestTag = "reparse_password_field"

@Composable
fun ReparseAllCard(
    onReparseClick: (String, (Result<ReparseSummary>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsRow(
        icon = "🔄",
        title = AppStrings.settingsRowReparseLabel,
        subtitle = AppStrings.settingsReparseSubtitle,
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        ReparseAllDialog(
            onReparseClick = onReparseClick,
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun ReparseAllDialog(
    onReparseClick: (String, (Result<ReparseSummary>) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<BackupStatus?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        title = { Text(AppStrings.settingsReparseDialogTitle) },
        text = {
            ReparseDialogContent(
                password = password,
                onPasswordChange = { password = it },
                isRunning = isRunning,
                status = status,
            )
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotBlank() && !isRunning,
                onClick = {
                    isRunning = true
                    status = null
                    onReparseClick(password) { result ->
                        isRunning = false
                        status = result.toBackupStatus()
                    }
                },
            ) {
                Text(AppStrings.settingsReparseConfirmBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isRunning) {
                Text(AppStrings.btnCancel)
            }
        },
    )
}

@Composable
private fun ReparseDialogContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    isRunning: Boolean,
    status: BackupStatus?,
) {
    var fieldValue by remember(password) {
        mutableStateOf(TextFieldValue(text = password, selection = TextRange(password.length)))
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(AppStrings.settingsReparseDialogBody, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = fieldValue,
            onValueChange = {
                fieldValue = it.copy(selection = TextRange(it.text.length))
                onPasswordChange(it.text)
            },
            label = { Text(AppStrings.settingsReparsePasswordLabel) },
            singleLine = true,
            enabled = !isRunning,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag(ReparsePasswordFieldTestTag),
        )
        if (isRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        status?.let { StatusMessage(status = it) }
    }
}

private fun Result<ReparseSummary>.toBackupStatus(): BackupStatus =
    fold(
        onSuccess = { summary ->
            val base = "${AppStrings.settingsReparseSuccessPrefix}${summary.succeeded}/${summary.total}"
            val failedNote =
                if (summary.failedDates.isEmpty()) {
                    ""
                } else {
                    " — ${AppStrings.settingsReparseFailedSuffix}${summary.failedDates.joinToString()}"
                }
            BackupStatus(message = base + failedNote, isSuccess = summary.failedDates.isEmpty())
        },
        onFailure = { e -> BackupStatus("${AppStrings.settingsReparseErrorPrefix}${e.message}", isSuccess = false) },
    )
