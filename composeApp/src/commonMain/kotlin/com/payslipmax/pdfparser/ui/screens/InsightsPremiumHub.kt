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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/**
 * The free-tier "mother" PRO card (Insights PRO consolidation, Phase 1): replaces the three scattered
 * teasers — [RecommendedActions], [AdvancedAnomaliesCard]'s locked branch, [PremiumReportCard]'s
 * teaser — with one hub. Each teaser's signal survives as a section here: the catalog bundle
 * ([premiumBundleHighlights]), the locked anomaly count/labels ([partitionAdvancedAnomalies], never
 * amounts — D6), and the top contextual CTA ([buildRecommendedActions]).
 */
@Composable
fun LockedPremiumHubCard(
    state: InsightsState,
    smartInsights: List<InsightUiModel>,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anomalyDisplay =
        remember(state.engineResult.anomalies) {
            partitionAdvancedAnomalies(state.engineResult.anomalies, hasAnomalyDetection = false)
        }
    val mostRelevant = remember(state, smartInsights) { buildRecommendedActions(state, smartInsights).firstOrNull() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            PremiumHubHeader()
            PremiumHubBundleList()
            if (anomalyDisplay.lockedCount > 0) PremiumHubAnomalyHook(anomalyDisplay)
            if (mostRelevant != null) PremiumHubMostRelevant(mostRelevant)
            Button(onClick = onUpgradeClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = InsightsStrings.premiumHubCta, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PremiumHubHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${AppStrings.proTeaserCrownIcon} ${InsightsStrings.premiumHubTitle}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = InsightsStrings.premiumIntelligencePrice,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun PremiumHubBundleList() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Text(
            text = InsightsStrings.premiumHubBundleIntro,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        premiumBundleHighlights().forEach { feature ->
            Text(
                text = "${feature.icon} ${feature.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PremiumHubAnomalyHook(display: AdvancedAnomalyDisplay) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
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
    }
}

@Composable
private fun PremiumHubMostRelevant(action: RecommendedActionUiModel) {
    Text(
        text = "${InsightsStrings.premiumHubMostRelevantPrefix}${action.title}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}
