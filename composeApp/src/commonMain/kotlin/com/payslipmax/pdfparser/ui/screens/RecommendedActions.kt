package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
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
                    icon = AppStringsPremium.premiumCatalogRetCalcIcon,
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
