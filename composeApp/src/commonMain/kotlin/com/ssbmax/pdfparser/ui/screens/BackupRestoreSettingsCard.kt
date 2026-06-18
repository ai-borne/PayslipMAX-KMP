@file:OptIn(ExperimentalMaterial3Api::class)

package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
    onCloudBackupClick: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onCloudRestoreClick: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
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
            onCloudBackupClick = onCloudBackupClick,
            onCloudRestoreClick = onCloudRestoreClick,
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
    onCloudBackupClick: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onCloudRestoreClick: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
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
            onCloudBackupClick = onCloudBackupClick,
            onCloudRestoreClick = onCloudRestoreClick,
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
    onCloudBackupClick: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onCloudRestoreClick: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
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
        CloudSyncWrapper(
            password = password,
            onCloudBackupClick = onCloudBackupClick,
            onCloudRestoreClick = onCloudRestoreClick,
            onStatusChange = { status = it },
        )
        status?.let { StatusMessage(status = it) }
    }
}

@Composable
private fun CloudSyncWrapper(
    password: String,
    onCloudBackupClick: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onCloudRestoreClick: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onStatusChange: (BackupStatus) -> Unit,
) {
    var cloudUserId by remember { mutableStateOf("") }
    var cloudAuthToken by remember { mutableStateOf("") }

    CloudSyncSection(
        userId = cloudUserId,
        onUserIdChange = { cloudUserId = it },
        authToken = cloudAuthToken,
        onAuthTokenChange = { cloudAuthToken = it },
        onCloudBackupClick = {
            onCloudBackupClick(cloudUserId, cloudAuthToken, password) { result ->
                val statusMsg =
                    if (result.isSuccess) {
                        com.ssbmax.pdfparser.ui.theme.CloudSyncStrings.statusCloudBackupSuccess
                    } else {
                        "${com.ssbmax.pdfparser.ui.theme.CloudSyncStrings.statusCloudSyncFailed}${result.exceptionOrNull()?.message}"
                    }
                onStatusChange(BackupStatus(statusMsg, isSuccess = result.isSuccess))
            }
        },
        onCloudRestoreClick = {
            onCloudRestoreClick(cloudUserId, cloudAuthToken, password) { result ->
                val statusMsg =
                    if (result.isSuccess) {
                        com.ssbmax.pdfparser.ui.theme.CloudSyncStrings.statusCloudRestoreSuccess
                    } else {
                        "${com.ssbmax.pdfparser.ui.theme.CloudSyncStrings.statusCloudSyncFailed}${result.exceptionOrNull()?.message}"
                    }
                onStatusChange(BackupStatus(statusMsg, isSuccess = result.isSuccess))
            }
        },
    )
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
