@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.database.hexToByteArray
import com.payslipmax.pdfparser.database.toHex
import com.payslipmax.pdfparser.ui.platform.rememberClipboardCopier
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun BackupRestoreSettingsCard(
    password: String,
    onPasswordChange: (String) -> Unit,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onImportBackup: (ByteArray, String, (Result<Unit>) -> Unit) -> Unit,
    canBackup: Boolean,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    // Restore is free (D3), so the sheet always opens; backup *creation* inside it is gated.
    val subtitleText =
        if (canBackup) {
            AppStrings.settingsStatusConfigured
        } else {
            AppStrings.settingsStatusBackupPro
        }

    SettingsRow(
        icon = "💾",
        title = AppStrings.settingsRowBackupLabel,
        subtitle = subtitleText,
        onClick = { showSheet = true },
        modifier = modifier,
    )

    if (showSheet) {
        BackupRestoreBottomSheet(
            password = password,
            onPasswordChange = onPasswordChange,
            canBackup = canBackup,
            onLockedBackup = {
                showSheet = false
                onUpgradePrompt()
            },
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
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
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
            canBackup = canBackup,
            onLockedBackup = onLockedBackup,
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
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onBackupClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestoreClick: (String, (Result<Unit>) -> Unit) -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onImportBackup: (ByteArray, String, (Result<Unit>) -> Unit) -> Unit,
    onCloseClick: () -> Unit,
) {
    var status by remember { mutableStateOf<BackupStatus?>(null) }
    val copyToClipboard = rememberClipboardCopier()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimensions.PaddingMedium)
                .padding(bottom = AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        BackupRestoreHeader(onCloseClick = onCloseClick)
        BackupRestorePasswordField(password = password, onPasswordChange = onPasswordChange)
        LocalSyncButtonsRow(password, canBackup, onLockedBackup, onBackupClick, onRestoreClick) { status = it }
        UniversalBackupSectionWrapper(
            password = password,
            canBackup = canBackup,
            onLockedBackup = onLockedBackup,
            copyToClipboard = copyToClipboard,
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
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    copyToClipboard: (String) -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onImportBackup: (ByteArray, String, (Result<Unit>) -> Unit) -> Unit,
    onStatusChange: (BackupStatus) -> Unit,
) {
    UniversalBackupSection(
        password = password,
        canBackup = canBackup,
        onLockedBackup = onLockedBackup,
        onExportClick = {
            onExportBackup(password) { result ->
                if (result.isSuccess) {
                    val hex = result.getOrThrow().toHex()
                    copyToClipboard(hex)
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
