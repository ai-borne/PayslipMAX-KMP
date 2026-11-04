@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.ui.backup.BackupRestoreViewModel
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.BackupCloudNote
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.launch

/**
 * The Backup & Restore row and its sheet. The sheet draws the [backup] view model's state (docs/Plan/14a P7); P8 replaces its body with the
 * progress panel, preview card and result card. A backup or restore runs on while Settings is off the screen (the app lock), so when the
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
        BackupRestoreBottomSheet(
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

@Composable
private fun BackupRestoreBottomSheet(
    state: BackupUiState,
    backup: BackupRestoreViewModel,
    contents: suspend () -> BackupContents,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BackupRestoreSheetContent(
            state = state,
            backup = backup,
            contents = contents,
            canBackup = canBackup,
            onLockedBackup = onLockedBackup,
            onPickBackup = onPickBackup,
            onCloseClick = onDismissRequest,
        )
    }
}

/** The password lives only here, in the open sheet: it is handed to each call and never kept by the view model or saved (14a section 4). */
@Composable
private fun BackupRestoreSheetContent(
    state: BackupUiState,
    backup: BackupRestoreViewModel,
    contents: suspend () -> BackupContents,
    canBackup: Boolean,
    onLockedBackup: () -> Unit,
    onPickBackup: (onResult: (String) -> Unit) -> Unit,
    onCloseClick: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var refusal by remember { mutableStateOf<BackupStatus?>(null) }
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
        BackupRestorePasswordField(password = password, onPasswordChange = { password = it })
        BackupCloudNote(visible = isPart2Enabled())
        BackupRestoreButtonsRow(
            canBackup = canBackup,
            onBackUp = { scope.launch { refusal = runBackup(canBackup, password, contents(), onLockedBackup, backup) } },
            onRestore = { refusal = runRestorePick(password, onPickBackup) { picked -> backup.restore(picked, password) } },
        )
        (refusal ?: state.status())?.let { StatusMessage(status = it) }
    }

    RestorePreviewPrompt(state = state, contents = contents, backup = backup)
}

/**
 * Today's question over the preview, until P8's preview card (D10): a device that holds nothing restores at once (Replace); otherwise
 * Replace or Merge is asked, and saying neither cancels the preview (the picked file is deleted).
 */
@Composable
private fun RestorePreviewPrompt(
    state: BackupUiState,
    contents: suspend () -> BackupContents,
    backup: BackupRestoreViewModel,
) {
    val preview = state as? BackupUiState.Preview ?: return
    var title by remember(preview) { mutableStateOf<String?>(null) }
    LaunchedEffect(preview) {
        val here = contents()
        if (restoreNeedsChoice(here)) title = restoreChoiceTitle(here) else backup.commit(RestoreMode.REPLACE)
    }
    RestoreCollisionDialog(
        title = title ?: return,
        onReplace = { backup.commit(RestoreMode.REPLACE) },
        onMerge = { backup.commit(RestoreMode.MERGE) },
        onDismiss = backup::cancel,
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
