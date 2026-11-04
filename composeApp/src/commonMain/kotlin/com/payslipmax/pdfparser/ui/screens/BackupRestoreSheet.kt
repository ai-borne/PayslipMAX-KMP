@file:OptIn(ExperimentalMaterial3Api::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.BackupCloudNote
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import kotlinx.coroutines.launch

const val BackUpButtonTag = "backup_back_up_button"
const val RestoreButtonTag = "backup_restore_button"
const val BackupConfirmFieldTag = "backup_confirm_field"
const val BackupLastBackupTag = "backup_last_backup"

/** What the sheet can ask for. Back Up and Restore answer with why they cannot start (no password, nothing to back up), or null. */
internal class BackupSheetActions(
    val onBackUp: suspend (password: String) -> BackupStatus?,
    val onRestore: (password: String) -> BackupStatus?,
    val onCommit: (RestoreMode) -> Unit,
    val onCancel: () -> Unit,
)

/**
 * The sheet state that refuses to hide while [mayHide] is false (D12): `confirmValueChange` is what the swipe-down and the scrim go
 * through, on Android and iOS alike. It reads the latest value, since the state is created once.
 */
@Composable
internal fun rememberBackupSheetState(mayHide: Boolean): SheetState {
    val latest by rememberUpdatedState(mayHide)
    return rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { target -> latest || target != SheetValue.Hidden })
}

/**
 * The modal sheet. While [mayHide] is false no gesture, scrim tap or Back closes it; there is no Cancel (D12). Two paths, each tested:
 * the swipe asks [sheetState] (`confirmValueChange`), and Back and the scrim reach `onDismissRequest` directly, so it ignores them.
 * No `BackHandler` on top: it is deprecated, and it could not change what the officer sees.
 */
@Composable
internal fun BackupRestoreSheet(
    sheetState: SheetState,
    mayHide: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (mayHide) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        content()
    }
}

/** The sheet drawn from [state] alone (it scrolls, so no button is ever out of reach): a preview replaces the form, and every other state shows the form with its progress or result above. */
@Composable
internal fun BackupRestoreSheetBody(
    state: BackupUiState,
    contents: BackupContents?,
    canBackup: Boolean,
    actions: BackupSheetActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = AppDimensions.PaddingMedium)
                .padding(bottom = AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        BackupRestoreHeader()
        if (state is BackupUiState.Preview) {
            RestorePreviewCard(state.preview)
            RestoreChoice(state.preview, contents, actions.onCommit, actions.onCancel)
        } else {
            BackupStateNotice(state)
            BackupForm(live = state.formIsLive(), canBackup = canBackup, actions = actions)
        }
    }
}

/** What the state says above the form: the last backup, the progress, the result, or the failure's one fixed line. */
@Composable
private fun BackupStateNotice(state: BackupUiState) {
    when (state) {
        is BackupUiState.Idle -> {
            val line = state.lastBackupAt?.let { BackupRestoreStrings.lastBackup(backupDateText(it)) } ?: BackupRestoreStrings.lastBackupNever
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag(BackupLastBackupTag))
        }
        is BackupUiState.Working -> BackupProgressPanel(state)
        is BackupUiState.Done -> BackupResultCard(state.summary)
        is BackupUiState.Failed -> StatusMessage(failedStatus(state.failure))
        is BackupUiState.Preview -> Unit
    }
}

/** Both passwords live here in `remember` state (never saved), are handed to each call and dropped with the sheet (14a section 4). */
@Composable
private fun BackupForm(
    live: Boolean,
    canBackup: Boolean,
    actions: BackupSheetActions,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var refusal by remember { mutableStateOf<BackupStatus?>(null) }
    val scope = rememberCoroutineScope()
    val mismatch = confirm.isNotEmpty() && !passwordsMatch(password, confirm)
    BackupRestorePasswordField(password = password, onPasswordChange = { password = it }, enabled = live)
    BackupConfirmPasswordField(confirm = confirm, mismatch = mismatch, onConfirmChange = { confirm = it }, enabled = live)
    BackupCloudNote(visible = isPart2Enabled())
    BackupButtonsRow(
        canBackup = canBackup,
        backUpEnabled = live && (!canBackup || passwordsMatch(password, confirm)),
        restoreEnabled = live,
        onBackUp = { scope.launch { refusal = actions.onBackUp(password) } },
        onRestore = { refusal = actions.onRestore(password) },
    )
    refusal?.let { StatusMessage(status = it) }
}

@Composable
private fun BackupButtonsRow(
    canBackup: Boolean,
    backUpEnabled: Boolean,
    restoreEnabled: Boolean,
    onBackUp: () -> Unit,
    onRestore: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Button(onClick = onBackUp, enabled = backUpEnabled, modifier = Modifier.weight(1f).testTag(BackUpButtonTag)) {
            Text(if (canBackup) AppStrings.settingsBackupSaveBtn else "🔒 ${AppStrings.settingsBackupSaveBtn}")
        }
        OutlinedButton(onClick = onRestore, enabled = restoreEnabled, modifier = Modifier.weight(1f).testTag(RestoreButtonTag)) {
            Text(AppStrings.settingsBackupRestoreBtn)
        }
    }
}
