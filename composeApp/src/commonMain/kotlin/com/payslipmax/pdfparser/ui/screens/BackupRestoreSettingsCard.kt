@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.RestoreReport
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.BackupCloudNote
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreSettingsCard(
    password: String,
    onPasswordChange: (String) -> Unit,
    contents: suspend () -> BackupContents,
    onExportBackup: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestore: (String, String, RestoreMode, (Result<RestoreReport>) -> Unit) -> Unit,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    onDiscardPicked: (String) -> Unit,
    canBackup: Boolean,
    onUpgradePrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
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
        BackupRestoreBottomSheet(
            password = password,
            onPasswordChange = onPasswordChange,
            contents = contents,
            canBackup = canBackup,
            onLockedBackup = {
                showSheet = false
                onUpgradePrompt()
            },
            onExportBackup = onExportBackup,
            onRestore = onRestore,
            onPickBackup = onPickBackup,
            onDiscardPicked = onDiscardPicked,
            onDismissRequest = { showSheet = false },
        )
    }
}

@Composable
private fun BackupRestoreBottomSheet(
    password: String,
    onPasswordChange: (String) -> Unit,
    contents: suspend () -> BackupContents,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onExportBackup: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestore: (String, String, RestoreMode, (Result<RestoreReport>) -> Unit) -> Unit,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    onDiscardPicked: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BackupRestoreSheetContent(
            password = password,
            onPasswordChange = onPasswordChange,
            contents = contents,
            canBackup = canBackup,
            onLockedBackup = onLockedBackup,
            onExportBackup = onExportBackup,
            onRestore = onRestore,
            onPickBackup = onPickBackup,
            onDiscardPicked = onDiscardPicked,
            onCloseClick = onDismissRequest,
        )
    }
}

@Composable
private fun BackupRestoreSheetContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    contents: suspend () -> BackupContents,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onExportBackup: (String, (Result<Unit>) -> Unit) -> Unit,
    onRestore: (String, String, RestoreMode, (Result<RestoreReport>) -> Unit) -> Unit,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    onDiscardPicked: (String) -> Unit,
    onCloseClick: () -> Unit,
) {
    var status by remember { mutableStateOf<BackupStatus?>(null) }
    var pendingRestore by remember { mutableStateOf<PendingRestore?>(null) }
    val scope = rememberCoroutineScope()

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
        BackupCloudNote(visible = isPart2Enabled())
        BackupRestoreButtonsRow(
            canBackup = canBackup,
            onBackUp = { scope.launch { runBackup(canBackup, password, contents(), onLockedBackup, onExportBackup) { status = it } } },
            onRestore = {
                runRestorePick(password, onPickBackup, { status = it }) { picked ->
                    scope.launch { restoreOrAsk(picked, password, contents(), onRestore, { status = it }) { pendingRestore = it } }
                }
            },
        )
        status?.let { StatusMessage(status = it) }
    }

    RestoreCollisionPrompt(
        pendingRestore = pendingRestore,
        password = password,
        onRestore = onRestore,
        onStatus = { status = it },
        onDiscard = onDiscardPicked,
        onClear = { pendingRestore = null },
    )
}

@Composable
private fun RestoreCollisionPrompt(
    pendingRestore: PendingRestore?,
    password: String,
    onRestore: (String, String, RestoreMode, (Result<RestoreReport>) -> Unit) -> Unit,
    onStatus: (BackupStatus) -> Unit,
    onDiscard: (String) -> Unit,
    onClear: () -> Unit,
) {
    val pending = pendingRestore ?: return
    val picked = pending.picked
    RestoreCollisionDialog(
        title = pending.title,
        onReplace = {
            onRestore(picked, password, RestoreMode.REPLACE) { onStatus(restoreStatus(it)) }
            onClear()
        },
        onMerge = {
            onRestore(picked, password, RestoreMode.MERGE) { onStatus(restoreStatus(it)) }
            onClear()
        },
        onDismiss = {
            onDiscard(picked)
            onClear()
        },
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
    title: String,
    onReplace: () -> Unit,
    onMerge: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(AppStrings.settingsRestoreExistingMsg) },
        confirmButton = { TextButton(onClick = onReplace) { Text(AppStrings.settingsRestoreReplaceBtn) } },
        dismissButton = { TextButton(onClick = onMerge) { Text(AppStrings.settingsRestoreMergeBtn) } },
    )
}

/** Runs the gated backup export; the view model writes the encrypted file and opens the OS share sheet on it. */
private fun runBackup(
    canBackup: Boolean,
    password: String,
    contents: BackupContents,
    onLockedBackup: () -> Unit,
    onExportBackup: (String, (Result<Unit>) -> Unit) -> Unit,
    onStatus: (BackupStatus) -> Unit,
) {
    if (!canBackup) return onLockedBackup()
    backupRefusal(password, contents)?.let { return onStatus(it) }
    onExportBackup(password) { result -> onStatus(backupStatus(result, contents)) }
}

/** The picked backup file waiting for the officer's Replace or Merge answer, and the question to ask. */
private class PendingRestore(
    val picked: String,
    val title: String,
)

/** Restores straight away on a fresh device; when something is already there, hands the picked file on with the question to ask. */
private fun restoreOrAsk(
    picked: String,
    password: String,
    here: BackupContents,
    onRestore: (String, String, RestoreMode, (Result<RestoreReport>) -> Unit) -> Unit,
    onStatus: (BackupStatus) -> Unit,
    onAsk: (PendingRestore) -> Unit,
) {
    if (restoreNeedsChoice(here)) {
        onAsk(PendingRestore(picked, restoreChoiceTitle(here)))
    } else {
        onRestore(picked, password, RestoreMode.REPLACE) { onStatus(restoreStatus(it)) }
    }
}

/**
 * Restore is free but still password-gated. Opens the file picker and hands the picked file on; the caller then restores straight
 * away (fresh device) or asks Replace or Merge when payslips or Part II items already exist ([restoreNeedsChoice]).
 */
private fun runRestorePick(
    password: String,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    onStatus: (BackupStatus) -> Unit,
    onPicked: (String) -> Unit,
) {
    if (password.isBlank()) {
        onStatus(BackupStatus(AppStrings.settingsBackupPasswordRequired, isSuccess = false))
        return
    }
    onPickBackup(onPicked)
}
