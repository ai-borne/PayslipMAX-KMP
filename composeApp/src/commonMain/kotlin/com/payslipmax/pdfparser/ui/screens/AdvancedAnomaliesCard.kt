package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * PRO advanced-anomaly surface (ANOMALY_DETECTION gate, D6) — unlocked findings only. The locked
 * teaser (category/count, no amounts) now lives solely in [LockedPremiumHubCard]; this card is only
 * ever reached from the PRO-dissolve path (Insights PRO consolidation, Phase 2). Renders nothing when
 * there are no PRO anomalies to show.
 */
@Composable
fun AdvancedAnomaliesCard(
    anomalies: List<Anomaly>,
    hasAnomalyDetection: Boolean,
    modifier: Modifier = Modifier,
) {
    val unlocked = remember(anomalies, hasAnomalyDetection) { partitionAdvancedAnomalies(anomalies, hasAnomalyDetection).unlocked }
    if (unlocked.isEmpty()) return

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
                text = InsightsStrings.advancedAnomaliesTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            unlocked.forEach { AnomalyDetailRow(anomaly = it) }
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
