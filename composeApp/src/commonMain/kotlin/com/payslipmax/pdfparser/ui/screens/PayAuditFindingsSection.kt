package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings

/**
 * Findings section of [PayAuditScreen]: unlocked findings render in full, including the evidence
 * ([Anomaly.expected]/[Anomaly.actual]/[Anomaly.authority]) Phase 2 attached but no surface has shown
 * until now (docs/Plan/09_PayAudit_PhasePlan.md Phase 7 carry-over, "Evidence is not stored or
 * shown... Phase 4/5"); locked shows only the count + category labels, per Phase 4's free-tier split.
 */
fun LazyListScope.payAuditFindingsItems(
    display: PayAuditFindingsDisplay,
    onUnlockClick: () -> Unit,
) {
    item(key = "pay_audit_findings_header", contentType = "section_header") {
        Text(
            text = PayAuditStrings.findingsSectionTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
    when {
        display.totalCount == 0 ->
            item(key = "pay_audit_findings_empty", contentType = "empty_state") {
                Text(text = PayAuditStrings.findingsEmptyState, style = MaterialTheme.typography.bodyMedium)
            }
        display.isLocked ->
            item(key = "pay_audit_findings_locked", contentType = "locked_teaser") {
                PayAuditLockedFindingsCard(display = display, onUnlockClick = onUnlockClick)
            }
        else ->
            itemsIndexed(
                display.unlocked,
                // (type, month) is not unique: the basic-DA and TPTA-DA arrears checks share both.
                key = { index, it -> "${it.type}_${it.month}_$index" },
                contentType = { _, _ -> "finding_row" },
            ) { _, anomaly ->
                PayAuditFindingRow(anomaly = anomaly)
            }
    }
}

@Composable
private fun PayAuditLockedFindingsCard(
    display: PayAuditFindingsDisplay,
    onUnlockClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            val countLabel = if (display.lockedCount == 1) PayAuditStrings.findingsCountSingular else PayAuditStrings.findingsCountPlural
            Text(text = "${display.lockedCount} $countLabel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                text = display.lockedLabels.joinToString(InsightsStrings.advancedAnomaliesLabelSeparator),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onUnlockClick) { Text(PayAuditStrings.findingsLockedCta) }
        }
    }
}

@Composable
private fun PayAuditFindingRow(anomaly: Anomaly) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = anomalyCategoryLabel(anomaly.type),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = anomaly.description, style = MaterialTheme.typography.bodyMedium)
            if (anomaly.isPending) {
                Text(
                    text = PayAuditStrings.findingsPendingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            val expected = anomaly.expected
            val actual = anomaly.actual
            if (expected != null && actual != null) {
                Text(
                    text = "${PayAuditStrings.findingsExpectedLabel}${expected.toInt()}   ${PayAuditStrings.findingsActualLabel}${actual.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            anomaly.authority?.let {
                Text(
                    text = "${PayAuditStrings.findingsAuthorityLabel}$it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
