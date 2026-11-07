package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.BackupColors
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings

const val RestorePreviewCardTag = "restore_preview_card"
const val RestorePreviewDateTag = "restore_preview_date"
const val RestorePreviewSkippedTag = "restore_preview_skipped"

/**
 * What the opened backup holds, before anything is written (invariant 4): when it was made (only files that carry it, D15), its format,
 * the counts, and the rows it could not read in the warning colour. Words say "holds", never "adds" (a Merge may change less).
 */
@Composable
internal fun RestorePreviewCard(preview: RestorePreview) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(RestorePreviewCardTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(BackupRestoreStrings.previewHolds(preview), style = MaterialTheme.typography.bodyLarge)
            preview.exportedAt?.let {
                Text(BackupRestoreStrings.previewMadeOn(backupDateText(it)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag(RestorePreviewDateTag))
            }
            Text(BackupRestoreStrings.previewFormat(preview.formatVersion), style = MaterialTheme.typography.bodySmall)
            if (preview.skipped.total > 0) {
                Text(
                    text = BackupRestoreStrings.previewSkipped(preview.skipped),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BackupColors.skipped(),
                    modifier = Modifier.testTag(RestorePreviewSkippedTag),
                )
            }
        }
    }
}
