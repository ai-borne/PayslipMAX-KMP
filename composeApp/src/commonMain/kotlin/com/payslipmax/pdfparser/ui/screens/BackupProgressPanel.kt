package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings

const val BackupProgressSpinnerTag = "backup_progress_spinner"
const val BackupProgressBarTag = "backup_progress_bar"

/** A spinner while the total is not known yet (the key is derived, the file opens), then a bar that fills, and the stage's fixed line (U1). */
@Composable
internal fun BackupProgressPanel(state: BackupUiState.Working) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        if (state.total <= 0) {
            CircularProgressIndicator(modifier = Modifier.testTag(BackupProgressSpinnerTag))
        } else {
            LinearProgressIndicator(
                progress = { (state.done.toFloat() / state.total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().testTag(BackupProgressBarTag),
            )
        }
        Text(
            text = BackupRestoreStrings.progressLine(state.op, state.stage, state.done, state.total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
