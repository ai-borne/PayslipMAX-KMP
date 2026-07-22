package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.RetirementCalculatorEngine
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun CommutationSimulatorCard(
    scenarios: List<RetirementCalculatorEngine.CommutationScenario>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                text = AppStringsPremium.retCommutationSimulatorTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = AppStringsPremium.retCommutationSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            ) {
                scenarios.forEach { scenario ->
                    ScenarioCard(scenario = scenario, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: RetirementCalculatorEngine.CommutationScenario,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = "${(scenario.fraction * 100).toInt()}% Commute",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTwo))
            ScenarioMetrics(scenario)
        }
    }
}

@Composable
private fun ScenarioMetrics(scenario: RetirementCalculatorEngine.CommutationScenario) {
    Text(
        text = AppStringsPremium.retCommutationLumpSumLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = formatShortCurrency(scenario.lumpSum),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = AppStringsPremium.retCommutationMonthlyNetLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = formatShortCurrency(scenario.netMonthlyPayout),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary,
    )
    if (scenario.breakEvenRoiPercent > 0.0) {
        RoiLabel(scenario.breakEvenRoiPercent)
    }
}

@Composable
private fun RoiLabel(roiPercent: Double) {
    Spacer(modifier = Modifier.height(AppDimensions.SpacingTwo))
    Text(
        text = AppStringsPremium.retCommutationReqRoiLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "${roiPercent.toString().take(4)}%",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

private fun formatShortCurrency(value: Double): String {
    return when {
        value >= 10000000.0 -> "₹${(value / 10000000.0).toString().take(4)} Cr"
        value >= 100000.0 -> "₹${(value / 100000.0).toString().take(4)} L"
        else -> formatCurrency(value)
    }
}
