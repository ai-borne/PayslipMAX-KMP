@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.backup.BackupRestoreViewModel
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.theme.AppStrings

/**
 * The Backup & Restore row and its sheet. The sheet draws the [backup] view model's state (docs/Plan/14a P7); The body is [BackupRestoreSheetBody] (progress panel, preview card, result card). A backup or restore runs on while Settings is off the screen (the app lock), so when the
 * view model has something the officer has not seen, the sheet opens again by itself ([opensTheSheet]).
 */
@Composable
fun BackupRestoreSettingsCard(
    backup: BackupRestoreViewModel,
    contents: suspend () -> BackupContents,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    canBackup: Boolean,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by backup.state.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    LaunchedEffect(state::class) { if (state.opensTheSheet()) showSheet = true }
    // Restore is free (D3), so the sheet always opens; backup *creation* inside it is gated.
    val subtitleText = if (canBackup) AppStrings.settingsStatusConfigured else AppStrings.settingsStatusBackupPremium

    SettingsRow(
        icon = "💾",
        title = AppStrings.settingsRowBackupLabel,
        subtitle = subtitleText,
        badge = if (canBackup) null else AppStrings.premiumBadgeTag,
        onClick = { showSheet = true },
        modifier = modifier,
    )

    if (showSheet) {
        BackupRestoreSheetHost(
            state = state,
            backup = backup,
            contents = contents,
            canBackup = canBackup,
            onLockedBackup = {
                showSheet = false
                onUpgradePrompt()
            },
            onPickBackup = onPickBackup,
            onDismissRequest = {
                showSheet = false
                backup.dismiss()
            },
        )
    }
}

/** Wires the sheet to the view model. The sheet cannot be dismissed while a backup or restore is working (D12). */
@Composable
private fun BackupRestoreSheetHost(
    state: BackupUiState,
    backup: BackupRestoreViewModel,
    contents: suspend () -> BackupContents,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val mayHide = state.sheetMayHide()
    val onDevice by produceState<BackupContents?>(null, state is BackupUiState.Preview) { value = if (state is BackupUiState.Preview) contents() else null }
    val actions =
        BackupSheetActions(
            onBackUp = { password -> runBackup(canBackup, password, contents(), onLockedBackup, backup) },
            onRestore = { password -> runRestorePick(password, onPickBackup) { picked -> backup.restore(picked, password) } },
            onCommit = backup::commit,
            onCancel = backup::cancel,
        )
    BackupRestoreSheet(sheetState = rememberBackupSheetState(mayHide), mayHide = mayHide, onDismiss = onDismissRequest) {
        BackupRestoreSheetBody(state = state, contents = onDevice, canBackup = canBackup, actions = actions)
    }
}

/** Starts the gated backup, or says why it cannot start (no password, nothing to back up); null once it has started. */
private fun runBackup(
    canBackup: Boolean,
    password: String,
    contents: BackupContents,
    onLockedBackup: () -> Unit,
    backup: BackupRestoreViewModel,
): BackupStatus? {
    if (!canBackup) {
        onLockedBackup()
        return null
    }
    backupRefusal(password, contents)?.let { return it }
    backup.backUp(password, contents)
    return null
}

/** Restore is free but still password-gated: opens the file picker and hands the picked file on, or says why it cannot (no password). */
private fun runRestorePick(
    password: String,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    onPicked: (String) -> Unit,
): BackupStatus? {
    if (password.isBlank()) return BackupStatus(AppStrings.settingsBackupPasswordRequired, isSuccess = false)
    onPickBackup(onPicked)
    return null
}
