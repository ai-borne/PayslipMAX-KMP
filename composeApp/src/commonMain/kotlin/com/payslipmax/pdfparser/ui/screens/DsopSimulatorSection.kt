package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.ProjectionMath
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun DsopSimulatorSection(
    initialBalance: Double,
    initialContribution: Double,
    modifier: Modifier = Modifier,
) {
    var monthlyContribution by remember { mutableStateOf(initialContribution.coerceIn(6000.0, 100000.0).toFloat()) }
    val annualContribution = monthlyContribution * 12
    val exceedsLimit = annualContribution > 500000f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = AppStrings.dsopSimulatorTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))

            DsopSliderControls(
                monthlyContribution = monthlyContribution,
                onValueChange = { monthlyContribution = it },
                exceedsLimit = exceedsLimit,
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))

            if (exceedsLimit) {
                TaxWarningTooltip()
            } else {
                TaxSafeTooltip()
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            ProjectionGrid(initialBalance, monthlyContribution.toDouble())

            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            DsopAssetComparisonCard(initialBalance, monthlyContribution.toDouble())
        }
    }
}

@Composable
private fun DsopSliderControls(
    monthlyContribution: Float,
    onValueChange: (Float) -> Unit,
    exceedsLimit: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = AppStrings.dsopSimulatorMonthlySub,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "₹${formatAmount(monthlyContribution.toDouble())}/mo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
        )
    }

    Slider(
        value = monthlyContribution,
        onValueChange = onValueChange,
        valueRange = 6000f..100000f,
        steps = 94,
        colors =
            SliderDefaults.colors(
                thumbColor = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                activeTrackColor = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ),
    )
}

@Composable
private fun TaxWarningTooltip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.06f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.SpacingTen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚠️", fontSize = AppDimensions.TextSizeExtraLarge, modifier = Modifier.padding(end = AppDimensions.SpacingSmall))
            Text(
                text = AppStrings.dsopSimulatorTaxWarning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TaxSafeTooltip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.SpacingTen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✅", fontSize = AppDimensions.TextSizeExtraLarge, modifier = Modifier.padding(end = AppDimensions.SpacingSmall))
            Text(
                text = AppStrings.dsopSimulatorTaxSafe,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ProjectionGrid(
    initialBalance: Double,
    monthly: Double,
) {
    val calculated1 = remember(initialBalance, monthly) { ProjectionMath.calculateProjection(initialBalance, monthly, 1).projectedBalance }
    val calculated5 = remember(initialBalance, monthly) { ProjectionMath.calculateProjection(initialBalance, monthly, 5).projectedBalance }
    val calculated10 = remember(initialBalance, monthly) { ProjectionMath.calculateProjection(initialBalance, monthly, 10).projectedBalance }
    val calculated15 = remember(initialBalance, monthly) { ProjectionMath.calculateProjection(initialBalance, monthly, 15).projectedBalance }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            ProjectionCard(AppStrings.dsopSimulator1Year, calculated1, Modifier.weight(1f))
            ProjectionCard(AppStrings.dsopSimulator5Years, calculated5, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            ProjectionCard(AppStrings.dsopSimulator10Years, calculated10, Modifier.weight(1f))
            ProjectionCard(AppStrings.dsopSimulator15Years, calculated15, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProjectionCard(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.SpacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTwo))
            Text(
                text = "₹${formatShortAmount(value)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatShortAmount(value: Double): String {
    return when {
        value >= 10000000.0 -> "${(value / 10000000.0).toString().take(4)} Cr"
        value >= 100000.0 -> "${(value / 100000.0).toString().take(4)} L"
        else -> value.toLong().toString()
    }
}
