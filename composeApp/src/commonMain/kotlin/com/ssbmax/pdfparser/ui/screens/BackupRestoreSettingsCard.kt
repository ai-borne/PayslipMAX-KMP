package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.database.hexToByteArray
import com.ssbmax.pdfparser.database.toHex
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun BackupRestoreSettingsCard(
    password: String,
    onPasswordChange: (String) -> Unit,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onImportBackup: (ByteArray, String, (Result<Unit>) -> Unit) -> Unit,
) {
    var status by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackupRestoreHeader()
            BackupRestorePasswordField(password = password, onPasswordChange = onPasswordChange)
            LocalSyncButtonsRow(password, onBackupClick, onRestoreClick) { status = it }
            UniversalBackupSection(
                password = password,
                onExportClick = {
                    onExportBackup(password) { result ->
                        if (result.isSuccess) {
                            val hex = result.getOrThrow().toHex()
                            clipboardManager.setText(AnnotatedString(hex))
                            status = AppStrings.statusCopiedSuccess
                        } else {
                            status = "${AppStrings.statusExportFailed}${result.exceptionOrNull()?.message}"
                        }
                    }
                },
                onImportClick = { hexStr ->
                    try {
                        val bytes = hexStr.trim().hexToByteArray()
                        onImportBackup(bytes, password) { result ->
                            status =
                                if (result.isSuccess) {
                                    AppStrings.statusRestoreComplete
                                } else {
                                    "${AppStrings.statusRestoreFailed}${result.exceptionOrNull()?.message}"
                                }
                        }
                    } catch (e: Exception) {
                        status = AppStrings.statusInvalidFormat
                    }
                },
            )
            status?.let { StatusMessage(message = it) }
        }
    }
}

@Composable
private fun BackupRestoreHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = AppStrings.settingsBackupHeader,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = AppStrings.settingsBackupDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BackupRestorePasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(AppStrings.labelPassword) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) "👁️" else "🙈")
            }
        },
    )
}

@Composable
private fun LocalSyncButtonsRow(
    password: String,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onStatusChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                onBackupClick(password) { result ->
                    onStatusChange(
                        if (result.isSuccess) {
                            AppStrings.statusSyncSuccess
                        } else {
                            "${AppStrings.statusSyncFailed}${result.exceptionOrNull()?.message}"
                        },
                    )
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.settingsBackupLocalBtn)
        }
        OutlinedButton(
            onClick = {
                onRestoreClick(password) { result ->
                    onStatusChange(
                        if (result.isSuccess) {
                            AppStrings.statusRestoreSuccess
                        } else {
                            "${AppStrings.statusRestoreFailed}${result.exceptionOrNull()?.message}"
                        },
                    )
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.settingsRestoreLocalBtn)
        }
    }
}

@Composable
private fun UniversalBackupSection(
    password: String,
    onExportClick: () -> Unit,
    onImportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var importText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Text(
            text = AppStrings.settingsBackupCrossPlatform,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onExportClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(AppStrings.settingsBackupExportBtn)
            }
            OutlinedButton(
                onClick = { isImporting = !isImporting },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (isImporting) {
                        AppStrings.settingsBackupCancelImportBtn
                    } else {
                        AppStrings.settingsBackupImportBtn
                    },
                )
            }
        }
        if (isImporting) {
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text(AppStrings.settingsBackupPasteLabel) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
            )
            Button(
                onClick = {
                    onImportClick(importText)
                    importText = ""
                    isImporting = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = importText.isNotBlank(),
            ) {
                Text(AppStrings.settingsBackupDecryptBtn)
            }
        }
    }
}

@Composable
private fun StatusMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color =
            if (message.contains("Success") || message.contains("Completed") || message.contains("Complete") || message.contains("Copied")) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.error
            },
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}
