package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/**
 * PRO advanced-anomaly surface (ANOMALY_DETECTION gate, D6). When unlocked, shows each high-value
 * auditor finding in full; when locked, shows only the check categories + count with an upgrade CTA
 * (no amounts, no descriptions leak). Renders nothing when there are no PRO anomalies.
 */
@Composable
fun AdvancedAnomaliesCard(
    anomalies: List<Anomaly>,
    hasAnomalyDetection: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = remember(anomalies, hasAnomalyDetection) { partitionAdvancedAnomalies(anomalies, hasAnomalyDetection) }
    if (!display.hasContent) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = if (display.isLocked) InsightsStrings.advancedAnomaliesLockedTitle else InsightsStrings.advancedAnomaliesTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (display.isLocked) {
                LockedAnomaliesTeaser(display = display, onUpgradeClick = onUpgradeClick)
            } else {
                display.unlocked.forEach { AnomalyDetailRow(anomaly = it) }
            }
        }
    }
}

@Composable
private fun AnomalyDetailRow(anomaly: Anomaly) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = AppDimensions.SpacingSmall),
            )
            Text(
                text = anomaly.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LockedAnomaliesTeaser(
    display: AdvancedAnomalyDisplay,
    onUpgradeClick: () -> Unit,
) {
    Text(
        text = "${display.lockedCount} ${InsightsStrings.advancedAnomaliesLockedCountSuffix}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = display.lockedLabels.joinToString(InsightsStrings.advancedAnomaliesLabelSeparator),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = InsightsStrings.advancedAnomaliesLockedBody,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = onUpgradeClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = InsightsStrings.advancedAnomaliesUnlockCta,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
