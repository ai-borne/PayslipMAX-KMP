package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.payslipmax.pdfparser.repository.RestoredRows
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.ui.backup.BackupSummary
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupColors
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings

const val BackupResultCardTag = "backup_result_card"

/** The lines of a finished restore: "Restore Complete!", what came back, and, only when rows were left out, which kinds (the loss must be noticed). */
internal fun restoreResultLines(
    restored: RestoredRows,
    skipped: SkippedRows,
): List<String> =
    listOfNotNull(AppStrings.statusRestoreComplete, BackupRestoreStrings.restoredCounts(restored), BackupRestoreStrings.restoredSkipped(skipped).takeIf { skipped.total > 0 })

/** What a finished backup or restore did: counts only, never amounts, months or names. */
@Composable
internal fun BackupResultCard(summary: BackupSummary) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(BackupResultCardTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        when (summary) {
            is BackupSummary.BackedUp ->
                Text(
                    text = backupDoneStatus(BackupContents(summary.payslips, summary.part2Items)).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            is BackupSummary.Restored -> {
                val lines = restoreResultLines(summary.restored, summary.skipped)
                lines.forEachIndexed { index, line ->
                    // The skipped-rows line, when there is one, is always the last.
                    val isLoss = summary.skipped.total > 0 && index == lines.lastIndex
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isLoss) BackupColors.skipped() else MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
