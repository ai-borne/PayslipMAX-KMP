package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.Screen
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.InsightsStrings
import kotlin.math.abs

@Composable
fun PremiumIntelligenceCard(
    isPremiumEnabled: Boolean,
    state: InsightsState,
    onUpgradeClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
    aiSectionContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isPremiumEnabled) {
        FreePremiumTeaser(
            state = state,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier
        )
    } else {
        ProPremiumSuite(
            state = state,
            onNavigateTo = onNavigateTo,
            aiSectionContent = aiSectionContent,
            modifier = modifier
        )
    }
}

@Composable
private fun FreePremiumTeaser(
    state: InsightsState,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TeaserHeaderRow()
            TeaserBodySection(state = state, onUpgradeClick = onUpgradeClick)
        }
    }
}

@Composable
private fun TeaserHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "👑 ${InsightsStrings.premiumIntelligenceTitle}",
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
private fun TeaserBodySection(
    state: InsightsState,
    onUpgradeClick: () -> Unit
) {
    val taxSaving = state.optimizationResult.totalPotentialTaxSaving
    val anomalies = state.engineResult.anomalies.filter { it.type in setOf("MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "SALARY_LOSS") }
    val recoveryOpportunity = anomalies.sumOf { it.amount }

    if (recoveryOpportunity > 0.0) {
        TeaserValueSection(
            title = InsightsStrings.potentialRecoveryOpportunityTitle,
            opportunityValue = formatCurrency(recoveryOpportunity),
            insight = anomalies.first().description,
            buttonLabel = InsightsStrings.unlockFullRepresentationsLabel,
            onClick = onUpgradeClick
        )
    } else if (taxSaving > 0.0) {
        val firstOpp = state.optimizationResult.opportunities.firstOrNull()?.action ?: ""
        TeaserValueSection(
            title = InsightsStrings.potentialTaxSavingsTitle,
            opportunityValue = "${formatCurrency(taxSaving)}/year",
            insight = firstOpp,
            buttonLabel = InsightsStrings.unlockFullRecommendationLabel,
            onClick = onUpgradeClick
        )
    } else {
        TeaserValueSection(
            title = "Premium Financial Analysis Found",
            opportunityValue = "Complete Financial Toolkit",
            insight = "Detailed projections, anomaly audits, and claims generators ready.",
            buttonLabel = "Unlock Premium Intelligence",
            onClick = onUpgradeClick
        )
    }
}

@Composable
private fun TeaserValueSection(
    title: String,
    opportunityValue: String,
    insight: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${InsightsStrings.estimatedOpportunityLabel} $opportunityValue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "• $insight",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProPremiumSuite(
    state: InsightsState,
    onNavigateTo: (Screen) -> Unit,
    aiSectionContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)
        ) {
            Text(
                text = "👑 ${InsightsStrings.premiumIntelligenceTitle} (Activated)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            // 1. Wealth Optimization Card
            val taxSaving = state.optimizationResult.totalPotentialTaxSaving
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(AppDimensions.PaddingSmall)) {
                    Text(
                        text = "💰 ${formatCurrency(taxSaving)} ${InsightsStrings.heroWealthSubLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. CA-Level AI Audit
            aiSectionContent()

            // 3. Premium Tools Navigation
            PremiumToolsSection(onNavigateTo = onNavigateTo)
        }
    }
}
