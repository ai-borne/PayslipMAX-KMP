package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.Screen
import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.insights.OptimizationResult
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.InsightsStrings

@Composable
fun AdaptiveHeroCard(
    engineResult: EngineResult,
    optimizationResult: OptimizationResult,
    isPremiumEnabled: Boolean,
    onNavigateTo: (Screen) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val branch = selectHeroBranch(engineResult, optimizationResult)) {
        is HeroBranch.Recovery ->
            RecoveryHeroCard(
                branch = branch,
                isPremiumEnabled = isPremiumEnabled,
                onCtaClick = { if (isPremiumEnabled) onNavigateTo(Screen.Representation) else onUpgradeClick() },
                modifier = modifier,
            )
        is HeroBranch.Wealth ->
            WealthHeroCard(
                branch = branch,
                isPremiumEnabled = isPremiumEnabled,
                onCtaClick = { if (isPremiumEnabled) onNavigateTo(Screen.TaxPlanning) else onUpgradeClick() },
                modifier = modifier,
            )
    }
}

@Composable
private fun RecoveryHeroCard(
    branch: HeroBranch.Recovery,
    isPremiumEnabled: Boolean,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                InsightsStrings.heroRecoverySectionTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
            )
            Text(
                "⚠ ${formatCurrency(branch.totalRecoverable)} ${InsightsStrings.heroRecoverySubLabel}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                branch.primaryCause,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            )
            if (branch.anomalies.size > 1) {
                Text(
                    "+ ${branch.anomalies.size - 1} ${InsightsStrings.heroRecoveryMoreIssuesSuffix}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(AppDimensions.SpacingSmall))
            RecoveryCtaButton(InsightsStrings.heroRecoveryCtaLabel, isPremiumEnabled, onCtaClick)
        }
    }
}

@Composable
private fun WealthHeroCard(
    branch: HeroBranch.Wealth,
    isPremiumEnabled: Boolean,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                InsightsStrings.heroWealthSectionTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Text(
                "💰 ${formatCurrency(branch.totalPotentialTaxSaving)} ${InsightsStrings.heroWealthSubLabel}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (branch.topOpportunityAction.isNotBlank()) {
                Text(
                    "• ${branch.topOpportunityAction}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Text(
                InsightsStrings.heroWealthRegimeDisclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(AppDimensions.SpacingSmall))
            WealthCtaButton(InsightsStrings.heroWealthCtaLabel, isPremiumEnabled, onCtaClick)
        }
    }
}

@Composable
private fun RecoveryCtaButton(
    label: String,
    isPremiumEnabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
    ) {
        Text(if (!isPremiumEnabled) "🔒 $label" else label)
    }
}

@Composable
private fun WealthCtaButton(
    label: String,
    isPremiumEnabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(if (!isPremiumEnabled) "🔒 $label" else label)
    }
}
