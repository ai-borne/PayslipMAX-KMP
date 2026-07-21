package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.TaxStoryNarrative
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun TaxNarrativeBenchmarkCard(
    narrative: TaxStoryNarrative,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            BenchmarkHeaderRow(parsedMonthCount = narrative.parsedMonthCount)

            Text(
                text = narrative.effectiveTaxRateMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            BenchmarkComparisonRow(
                yourRate = narrative.effectiveTaxRatePct,
                peerBenchmark = narrative.peerBenchmarkRatePct,
            )

            Text(
                text = "💡 Best achievable rate for your income tier is ${narrative.peerBenchmarkRatePct}% after Sec 80CCD(1B) NPS + DSOP + Sec 10(14) field exemptions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BenchmarkHeaderRow(parsedMonthCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = AppStringsPremium.taxPlanningNarrativeBenchmarkTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Text(
                text = "${AppStringsPremium.taxPlanningPreliminaryEstimatePrefix}$parsedMonthCount${AppStringsPremium.taxPlanningOfMonthsSuffix}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingTiny),
            )
        }
    }
}

@Composable
private fun BenchmarkComparisonRow(
    yourRate: Double,
    peerBenchmark: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        BenchmarkBadgeBox(
            label = AppStringsPremium.taxPlanningNarrativeYourRateLabel,
            rateText = "$yourRate%",
            isHighlight = false,
            modifier = Modifier.weight(1f),
        )
        BenchmarkBadgeBox(
            label = AppStringsPremium.taxPlanningBestAchievableRateLabel,
            rateText = "$peerBenchmark%",
            isHighlight = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BenchmarkBadgeBox(
    label: String,
    rateText: String,
    isHighlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isHighlight) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
            ),
        border =
            if (isHighlight) {
                BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = rateText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
