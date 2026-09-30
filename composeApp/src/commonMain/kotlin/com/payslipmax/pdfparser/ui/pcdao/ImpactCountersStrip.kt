package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCard
import com.payslipmax.pdfparser.ui.screens.formatCurrency

/**
 * Top KPI summary strip displaying Unclaimed Dues, Recovery Hazards, and Critical Alarms.
 */
@Composable
fun ImpactCountersStrip(
    unclaimedAmount: Double,
    hazardAmount: Double,
    criticalAlarmsCount: Int,
    modifier: Modifier = Modifier,
    onHazardCardClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KpiCard(
            title = AppStringsPcdao.kpiUnclaimedTitle,
            value = formatCurrency(unclaimedAmount),
            subtitle = AppStringsPcdao.kpiUnclaimedSubtitle,
            valueColor = Color(0xFF34D399),
            modifier = Modifier.weight(1f),
        )
        KpiCard(
            title = AppStringsPcdao.kpiHazardsTitle,
            value = formatCurrency(hazardAmount),
            subtitle = AppStringsPcdao.kpiHazardsSubtitle,
            valueColor = if (hazardAmount > 0.0) Color(0xFFF87171) else Color(0xFF94A3B8),
            modifier =
                Modifier
                    .weight(1f)
                    .testTag(TestTags.HAZARDS_KPI_CARD),
            onClick = onHazardCardClick,
        )
        KpiCard(
            title = AppStringsPcdao.kpiAlarmsTitle,
            value = if (criticalAlarmsCount == 1) "1${AppStringsPcdao.alarmSingularSuffix}" else "$criticalAlarmsCount${AppStringsPcdao.alarmsCountSuffix}",
            subtitle = AppStringsPcdao.kpiAlarmsSubtitle,
            valueColor = if (criticalAlarmsCount > 0) Color(0xFFFBBF24) else Color(0xFF94A3B8),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    FlatBorderedCard(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        tint = CardTint.Neutral,
        contentSpacing = 4.dp,
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
            )
        }
    }
}
