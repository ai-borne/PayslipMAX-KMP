@file:OptIn(ExperimentalMaterial3Api::class)

package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
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
    isPremiumEnabled: Boolean,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val subtitleText =
        if (isPremiumEnabled) {
            AppStrings.settingsStatusConfigured
        } else {
            AppStrings.settingsStatusProOnly
        }

    SettingsRow(
        icon = "☁️",
        title = AppStrings.settingsRowBackupLabel,
        subtitle = subtitleText,
        onClick = {
            if (isPremiumEnabled) {
                showSheet = true
            } else {
                onUpgradePrompt()
            }
        },
        modifier = modifier,
    )

    if (showSheet) {
        BackupRestoreBottomSheet(
            password = password,
            onPasswordChange = onPasswordChange,
            onBackupClick = onBackupClick,
            onRestoreClick = onRestoreClick,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onDismissRequest = { showSheet = false },
        )
    }
}

@Composable
private fun BackupRestoreBottomSheet(
    password: String,
    onPasswordChange: (String) -> Unit,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onImportBackup: (ByteArray, String, (Result<Unit>) -> Unit) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BackupRestoreSheetContent(
            password = password,
            onPasswordChange = onPasswordChange,
            onBackupClick = onBackupClick,
            onRestoreClick = onRestoreClick,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onCloseClick = onDismissRequest,
        )
    }
}

@Composable
private fun BackupRestoreSheetContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onImportBackup: (ByteArray, String, (Result<Unit>) -> Unit) -> Unit,
    onCloseClick: () -> Unit,
) {
    var status by remember { mutableStateOf<BackupStatus?>(null) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimensions.PaddingMedium)
                .padding(bottom = AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackupRestoreHeader(onCloseClick = onCloseClick)
        BackupRestorePasswordField(password = password, onPasswordChange = onPasswordChange)
        LocalSyncButtonsRow(password, onBackupClick, onRestoreClick) { status = it }
        UniversalBackupSectionWrapper(
            password = password,
            clipboardManager = clipboardManager,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onStatusChange = { status = it },
        )
        status?.let { StatusMessage(status = it) }
    }
}

@Composable
private fun UniversalBackupSectionWrapper(
    password: String,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onImportBackup: (ByteArray, String, (Result<Unit>) -> Unit) -> Unit,
    onStatusChange: (BackupStatus) -> Unit,
) {
    UniversalBackupSection(
        password = password,
        onExportClick = {
            onExportBackup(password) { result ->
                if (result.isSuccess) {
                    val hex = result.getOrThrow().toHex()
                    clipboardManager.setText(AnnotatedString(hex))
                    onStatusChange(BackupStatus(AppStrings.statusCopiedSuccess, isSuccess = true))
                } else {
                    onStatusChange(
                        BackupStatus(
                            "${AppStrings.statusExportFailed}${result.exceptionOrNull()?.message}",
                            isSuccess = false,
                        ),
                    )
                }
            }
        },
        onImportClick = { hexStr ->
            try {
                val bytes = hexStr.trim().hexToByteArray()
                onImportBackup(bytes, password) { result ->
                    val statusMsg =
                        if (result.isSuccess) {
                            AppStrings.statusRestoreComplete
                        } else {
                            "${AppStrings.statusRestoreFailed}${result.exceptionOrNull()?.message}"
                        }
                    onStatusChange(BackupStatus(statusMsg, isSuccess = result.isSuccess))
                }
            } catch (e: Exception) {
                onStatusChange(BackupStatus(AppStrings.statusInvalidFormat, isSuccess = false))
            }
        },
    )
}

@Composable
private fun BackupRestoreHeader(onCloseClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
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
    onStatusChange: (BackupStatus) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                onBackupClick(password) { result ->
                    val statusMsg =
                        if (result.isSuccess) {
                            AppStrings.statusSyncSuccess
                        } else {
                            "${AppStrings.statusSyncFailed}${result.exceptionOrNull()?.message}"
                        }
                    onStatusChange(BackupStatus(statusMsg, isSuccess = result.isSuccess))
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.settingsBackupLocalBtn)
        }
        OutlinedButton(
            onClick = {
                onRestoreClick(password) { result ->
                    val statusMsg =
                        if (result.isSuccess) {
                            AppStrings.statusRestoreSuccess
                        } else {
                            "${AppStrings.statusRestoreFailed}${result.exceptionOrNull()?.message}"
                        }
                    onStatusChange(BackupStatus(statusMsg, isSuccess = result.isSuccess))
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.settingsRestoreLocalBtn)
        }
    }
}
