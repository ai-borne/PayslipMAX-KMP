package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.DataConfidenceLevel
import com.payslipmax.pdfparser.insights.RetirementPlannerResult
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun RetirementHeroSummaryCard(
    result: RetirementPlannerResult,
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
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            HeroHeader(confidenceLevel = result.confidenceLevel)
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            HeroMonthlySection(
                netMonthlyPension = result.netMonthlyPensionCommuted50,
                basicPension = result.basicPension,
                daPercentage = result.daPercentage,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTwo))
            HeroCorpusSection(totalCorpus = result.totalDay1Corpus)
        }
    }
}

@Composable
private fun HeroHeader(confidenceLevel: DataConfidenceLevel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = AppStringsPremium.retHeroTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f, fill = false),
        )
        ConfidenceBadge(confidenceLevel)
    }
}

@Composable
private fun HeroMonthlySection(
    netMonthlyPension: Double,
    basicPension: Double,
    daPercentage: Double,
) {
    val residualPension = basicPension * 0.50
    val dearnessRelief = basicPension * (daPercentage / 100.0)
    val drPercentText = if (daPercentage > 0) "${daPercentage.toInt()}%" else "50%"

    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(
            text = AppStringsPremium.retHeroMonthlyPayoutLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${formatCurrency(netMonthlyPension)} / mo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${formatCurrency(residualPension)} (50% Pension) + ${formatCurrency(dearnessRelief)} (100% DR @ $drPercentText)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun HeroCorpusSection(totalCorpus: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(
            text = AppStringsPremium.retHeroDay1CorpusLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatCurrency(totalCorpus),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = "(DSOP + Gratuity + Leave + AGIF + Commutation Lump Sum)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun ConfidenceBadge(level: DataConfidenceLevel) {
    val (text, color) =
        when (level) {
            DataConfidenceLevel.HIGH -> AppStringsPremium.taxPlanningConfidenceHigh to MaterialTheme.colorScheme.primary
            DataConfidenceLevel.MODERATE -> AppStringsPremium.taxPlanningConfidenceModerate to MaterialTheme.colorScheme.secondary
            DataConfidenceLevel.PRELIMINARY -> AppStringsPremium.taxPlanningConfidencePreliminary to MaterialTheme.colorScheme.tertiary
        }
    Surface(
        shape = RoundedCornerShape(AppDimensions.CornerRadiusSmall),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(AppDimensions.BorderThin, color.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingTwo),
        )
    }
}
