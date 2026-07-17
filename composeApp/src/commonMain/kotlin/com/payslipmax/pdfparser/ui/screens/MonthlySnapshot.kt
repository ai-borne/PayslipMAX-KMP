package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import kotlin.math.abs

/** Month-over-month percent change, or null when there's no (or a zero) previous value to compare against. */
fun snapshotMomPercent(
    current: Double,
    previous: Double?,
): Double? {
    if (previous == null || previous == 0.0) return null
    return ((current - previous) / previous) * 100.0
}

/**
 * Snapshot hero: net-pay headline + gross/deductions/tax metrics (each with a MoM delta), plus the
 * single Pay Health surface for the whole screen — an expandable chip reusing [breakdownWellnessDrivers].
 */
@Composable
fun MonthlySnapshot(
    state: InsightsState,
    wellnessExpanded: Boolean,
    onWellnessExpandClick: () -> Unit,
    onSeeHowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = statusFor(state.engineResult.healthScore)
    val color = statusColor(status)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, color.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            SnapshotNetPayHero(current = state.currentRecord, previous = state.previousRecord)
            SnapshotMetricsRow(current = state.currentRecord, previous = state.previousRecord)
            SnapshotHealthChip(
                state = state,
                status = status,
                color = color,
                expanded = wellnessExpanded,
                onExpandClick = onWellnessExpandClick,
                onSeeHowClick = onSeeHowClick,
            )
        }
    }
}

@Composable
private fun SnapshotNetPayHero(
    current: LedgerRecordEntity,
    previous: LedgerRecordEntity?,
) {
    Column {
        Text(
            text = InsightsStrings.snapshotNetPayLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = formatCurrency(current.netPay),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SnapshotDeltaLabel(current = current.netPay, previous = previous?.netPay)
        }
    }
}

@Composable
private fun SnapshotDeltaLabel(
    current: Double,
    previous: Double?,
) {
    val pct = snapshotMomPercent(current, previous) ?: return
    if (abs(pct) < 0.1) return
    val improved = pct > 0
    val color = if (improved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    val arrow = if (improved) "↑" else "↓"
    Text(
        text = "$arrow ${abs(pct).toString().take(4)}%",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
private fun SnapshotMetricsRow(
    current: LedgerRecordEntity,
    previous: LedgerRecordEntity?,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SnapshotMetric(InsightsStrings.snapshotGrossPayLabel, current.grossPay, previous?.grossPay)
        SnapshotMetric(
            InsightsStrings.snapshotDeductionsLabel,
            current.grossPay - current.netPay,
            previous?.let { it.grossPay - it.netPay },
        )
        SnapshotMetric(InsightsStrings.snapshotTaxLabel, current.incomeTax, previous?.incomeTax)
    }
}

@Composable
private fun SnapshotMetric(
    label: String,
    value: Double,
    previousValue: Double?,
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = formatCurrency(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SnapshotDeltaLabel(current = value, previous = previousValue)
    }
}

@Composable
private fun SnapshotHealthChip(
    state: InsightsState,
    status: HealthStatus,
    color: Color,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    onSeeHowClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandClick),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        SnapshotHealthChipHeader(state = state, status = status, color = color, expanded = expanded)
        if (expanded) {
            SnapshotHealthChipBreakdown(state = state, onSeeHowClick = onSeeHowClick)
        }
    }
}

@Composable
private fun SnapshotHealthChipHeader(
    state: InsightsState,
    status: HealthStatus,
    color: Color,
    expanded: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "${InsightsStrings.wellnessChipLabel}: ${state.engineResult.healthScore}% · ${status.label}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            val trend = getTrendText(state.scoreDelta, state.previousMonthLabel)
            if (trend.isNotEmpty()) {
                Text(text = trend, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription =
                if (expanded) InsightsStrings.wellnessChipCollapseDesc else InsightsStrings.wellnessChipExpandDesc,
            tint = color,
        )
    }
}

@Composable
private fun SnapshotHealthChipBreakdown(
    state: InsightsState,
    onSeeHowClick: () -> Unit,
) {
    val drivers = remember(state.engineResult) { breakdownWellnessDrivers(state.engineResult) }
    val positive = drivers.filter { it.pointImpact >= 0 }
    val watch = drivers.filter { it.pointImpact < 0 }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (positive.isNotEmpty()) SnapshotDriverGroup(InsightsStrings.positiveSignalsTitle, positive)
        if (watch.isNotEmpty()) SnapshotDriverGroup(InsightsStrings.watchItemsTitle, watch)
        SnapshotOpportunityRow(amount = state.optimizationResult.totalPotentialTaxSaving, onSeeHowClick = onSeeHowClick)
    }
}

@Composable
private fun SnapshotDriverGroup(
    title: String,
    drivers: List<WellnessDriver>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        drivers.forEach { driver -> WellnessDriverRow(driver = driver) }
    }
}

@Composable
private fun SnapshotOpportunityRow(
    amount: Double,
    onSeeHowClick: () -> Unit,
) {
    if (amount <= 0.0) return
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(
            text = InsightsStrings.opportunityTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = formatCurrency(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onSeeHowClick) { Text(InsightsStrings.heroWealthCtaLabel) }
        }
    }
}
