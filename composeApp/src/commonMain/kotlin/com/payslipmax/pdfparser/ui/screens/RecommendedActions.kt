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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

data class RecommendedActionUiModel(
    val gate: FeatureGate,
    val icon: String,
    val title: String,
    val description: String,
    val actionLabel: String,
    val target: Screen,
)

private fun candidateRecommendedActions(state: InsightsState): List<RecommendedActionUiModel> =
    buildList {
        if (state.optimizationResult.totalPotentialTaxSaving > 0.0) {
            add(
                RecommendedActionUiModel(
                    gate = FeatureGate.TAX_PLANNER,
                    icon = InsightsStrings.premiumToolsTaxPlannerIcon,
                    title = InsightsStrings.recommendedActionTaxPlannerTitle,
                    description = InsightsStrings.recommendedActionTaxPlannerDesc,
                    actionLabel = InsightsStrings.premiumToolsOpenLabel,
                    target = Screen.TaxPlanning,
                ),
            )
        }
        if (state.engineResult.anomalies.any { it.type in REPRESENTATION_DRAFT_TYPES }) {
            add(
                RecommendedActionUiModel(
                    gate = FeatureGate.CLAIM_GENERATOR,
                    icon = InsightsStrings.premiumToolsDraftClaimsIcon,
                    title = InsightsStrings.recommendedActionClaimGeneratorTitle,
                    description = InsightsStrings.recommendedActionClaimGeneratorDesc,
                    actionLabel = InsightsStrings.premiumToolsOpenLabel,
                    target = Screen.Representation,
                ),
            )
        }
        if (state.optimizationResult.dsopGapMonthly > 0.0) {
            add(
                RecommendedActionUiModel(
                    gate = FeatureGate.DSOP_SIMULATOR,
                    icon = InsightsStrings.premiumToolsDsopIcon,
                    title = InsightsStrings.recommendedActionDsopSimulatorTitle,
                    description = InsightsStrings.recommendedActionDsopSimulatorDesc,
                    actionLabel = InsightsStrings.premiumToolsOpenLabel,
                    target = Screen.RetirementPlanning,
                ),
            )
        }
        if (state.currentRecord.dsopSubscription > 0.0) {
            add(
                RecommendedActionUiModel(
                    gate = FeatureGate.RETIREMENT_CALCULATORS,
                    icon = AppStringsPremium.proCatalogRetCalcIcon,
                    title = InsightsStrings.recommendedActionRetirementCalcTitle,
                    description = InsightsStrings.recommendedActionRetirementCalcDesc,
                    actionLabel = InsightsStrings.premiumToolsOpenLabel,
                    target = Screen.RetirementCalculators,
                ),
            )
        }
    }

/**
 * Data-condition-gated CTA candidates, minus any whose [RecommendedActionUiModel.target] is already
 * reachable from a visible Smart Insights card — the approved "no CTA twice" dedup rule.
 */
fun buildRecommendedActions(
    state: InsightsState,
    shownInsights: List<InsightUiModel>,
): List<RecommendedActionUiModel> {
    val shownTargets = shownInsights.mapNotNull { it.actionTarget }.toSet()
    return candidateRecommendedActions(state).filter { it.target !in shownTargets }
}

/** Contextual premium CTAs not already surfaced by a Smart Insights card. Renders nothing when empty. */
@Composable
fun RecommendedActions(
    state: InsightsState,
    shownInsights: List<InsightUiModel>,
    hasAccess: (FeatureGate) -> Boolean,
    onNavigateTo: (Screen) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = remember(state, shownInsights) { buildRecommendedActions(state, shownInsights) }
    if (actions.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                text = InsightsStrings.recommendedActionsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            actions.forEach { action ->
                RecommendedActionRow(
                    action = action,
                    onClick = { if (hasAccess(action.gate)) onNavigateTo(action.target) else onUpgradeClick() },
                )
            }
        }
    }
}

@Composable
private fun RecommendedActionRow(
    action: RecommendedActionUiModel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        Text(text = action.icon, style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            Text(text = action.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = action.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onClick) {
            Text(text = action.actionLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}
