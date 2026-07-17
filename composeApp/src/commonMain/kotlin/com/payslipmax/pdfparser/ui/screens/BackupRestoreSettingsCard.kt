@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.utils.shareBytes

// A backup is an opaque encrypted archive; octet-stream lets the OS share sheet offer every
// destination (Files, Drive, iCloud, email) rather than filtering by a document type.
private const val BACKUP_MIME = "application/octet-stream"
private const val BACKUP_FILE_NAME = "PayslipMax-backup.pcda"

@Composable
fun BackupRestoreSettingsCard(
    password: String,
    onPasswordChange: (String) -> Unit,
    payslipCount: Int,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onRestore: (ByteArray, String, RestoreMode, (Result<Unit>) -> Unit) -> Unit,
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit,
    canBackup: Boolean,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    // Restore is free (D3), so the sheet always opens; backup *creation* inside it is gated.
    val subtitleText = if (canBackup) AppStrings.settingsStatusConfigured else AppStrings.settingsStatusBackupPro

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
            payslipCount = payslipCount,
            canBackup = canBackup,
            onLockedBackup = {
                showSheet = false
                onUpgradePrompt()
            },
            onExportBackup = onExportBackup,
            onRestore = onRestore,
            onPickBackup = onPickBackup,
            onDismissRequest = { showSheet = false },
        )
    }
}

@Composable
private fun BackupRestoreBottomSheet(
    password: String,
    onPasswordChange: (String) -> Unit,
    payslipCount: Int,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onRestore: (ByteArray, String, RestoreMode, (Result<Unit>) -> Unit) -> Unit,
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BackupRestoreSheetContent(
            password = password,
            onPasswordChange = onPasswordChange,
            payslipCount = payslipCount,
            canBackup = canBackup,
            onLockedBackup = onLockedBackup,
            onExportBackup = onExportBackup,
            onRestore = onRestore,
            onPickBackup = onPickBackup,
            onCloseClick = onDismissRequest,
        )
    }
}

@Composable
private fun BackupRestoreSheetContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    payslipCount: Int,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onRestore: (ByteArray, String, RestoreMode, (Result<Unit>) -> Unit) -> Unit,
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit,
    onCloseClick: () -> Unit,
) {
    var status by remember { mutableStateOf<BackupStatus?>(null) }
    // Holds the picked backup bytes while the Replace/Merge dialog is up (device already has data).
    var pendingRestore by remember { mutableStateOf<ByteArray?>(null) }

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
        BackupRestoreButtonsRow(
            canBackup = canBackup,
            onBackUp = { runBackup(canBackup, password, payslipCount, onLockedBackup, onExportBackup) { status = it } },
            onRestore = {
                runRestorePick(password, payslipCount, onPickBackup, onRestore, { status = it }) { pendingRestore = it }
            },
        )
        status?.let { StatusMessage(status = it) }
    }

    RestoreCollisionPrompt(
        pendingRestore = pendingRestore,
        password = password,
        onRestore = onRestore,
        onStatus = { status = it },
        onClear = { pendingRestore = null },
    )
}

@Composable
private fun RestoreCollisionPrompt(
    pendingRestore: ByteArray?,
    password: String,
    onRestore: (ByteArray, String, RestoreMode, (Result<Unit>) -> Unit) -> Unit,
    onStatus: (BackupStatus) -> Unit,
    onClear: () -> Unit,
) {
    val bytes = pendingRestore ?: return
    RestoreCollisionDialog(
        onReplace = {
            onRestore(bytes, password, RestoreMode.REPLACE) { onStatus(restoreStatus(it)) }
            onClear()
        },
        onMerge = {
            onRestore(bytes, password, RestoreMode.MERGE) { onStatus(restoreStatus(it)) }
            onClear()
        },
        onDismiss = onClear,
    )
}

@Composable
private fun BackupRestoreButtonsRow(
    canBackup: Boolean,
    onBackUp: () -> Unit,
    onRestore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Button(onClick = onBackUp, modifier = Modifier.weight(1f)) {
            Text(if (canBackup) AppStrings.settingsBackupSaveBtn else "🔒 ${AppStrings.settingsBackupSaveBtn}")
        }
        OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
            Text(AppStrings.settingsBackupRestoreBtn)
        }
    }
}

@Composable
private fun RestoreCollisionDialog(
    onReplace: () -> Unit,
    onMerge: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.settingsRestoreExistingTitle) },
        text = { Text(AppStrings.settingsRestoreExistingMsg) },
        confirmButton = { TextButton(onClick = onReplace) { Text(AppStrings.settingsRestoreReplaceBtn) } },
        dismissButton = { TextButton(onClick = onMerge) { Text(AppStrings.settingsRestoreMergeBtn) } },
    )
}

/** Runs the gated backup export, then hands the encrypted bytes to the OS share sheet. */
private fun runBackup(
    canBackup: Boolean,
    password: String,
    payslipCount: Int,
    onLockedBackup: () -> Unit,
    onExportBackup: (String, (Result<ByteArray>) -> Unit) -> Unit,
    onStatus: (BackupStatus) -> Unit,
) {
    when {
        !canBackup -> onLockedBackup()
        password.isBlank() -> onStatus(BackupStatus(AppStrings.settingsBackupPasswordRequired, isSuccess = false))
        payslipCount == 0 -> onStatus(BackupStatus(AppStrings.statusNoPayslipsToBackup, isSuccess = false))
        else ->
            onExportBackup(password) { result ->
                if (result.isSuccess) {
                    shareBytes(result.getOrThrow(), BACKUP_FILE_NAME, BACKUP_MIME)
                    onStatus(BackupStatus("$payslipCount ${AppStrings.labelPayslipsBackedUp}", isSuccess = true))
                } else {
                    onStatus(BackupStatus("${AppStrings.statusBackupFailed}${result.exceptionOrNull()?.message}", isSuccess = false))
                }
            }
    }
}

/**
 * Restore is free but still password-gated. Opens the file picker, then either restores straight
 * away (fresh device) or defers to the Replace/Merge dialog when payslips already exist.
 */
private fun runRestorePick(
    password: String,
    payslipCount: Int,
    onPickBackup: (onResult: (ByteArray) -> Unit) -> Unit,
    onRestore: (ByteArray, String, RestoreMode, (Result<Unit>) -> Unit) -> Unit,
    onStatus: (BackupStatus) -> Unit,
    onNeedsChoice: (ByteArray) -> Unit,
) {
    if (password.isBlank()) {
        onStatus(BackupStatus(AppStrings.settingsBackupPasswordRequired, isSuccess = false))
        return
    }
    onPickBackup { bytes ->
        if (payslipCount > 0) {
            onNeedsChoice(bytes)
        } else {
            onRestore(bytes, password, RestoreMode.REPLACE) { onStatus(restoreStatus(it)) }
        }
    }
}

private fun restoreStatus(result: Result<Unit>): BackupStatus =
    if (result.isSuccess) {
        BackupStatus(AppStrings.statusRestoreComplete, isSuccess = true)
    } else {
        BackupStatus("${AppStrings.statusRestoreFailed}${result.exceptionOrNull()?.message}", isSuccess = false)
    }
