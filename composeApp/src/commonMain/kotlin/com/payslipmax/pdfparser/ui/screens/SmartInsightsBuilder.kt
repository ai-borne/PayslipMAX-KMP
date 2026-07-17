package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.AnomalySeverityMapper
import com.payslipmax.pdfparser.insights.InsightPrioritizationEngine
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.insights.Opportunity
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/** [Anomaly.type]s for which [com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository]
 *  already auto-generates a representation draft — reused here as the SSOT for which anomaly cards
 *  route to the Claim-Generator screen. */
private val REPRESENTATION_DRAFT_TYPES = setOf("SALARY_LOSS", "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT")

/**
 * Pure builder: [InsightsState] engine output -> ordered [InsightUiModel] cards for the redesigned
 * Smart Insights section. Not wired to the screen yet (Phase 4) — this is presentation logic only,
 * with no I/O and no engine re-computation (anomaly gating/suppression is already the auditors'
 * responsibility; this function is a pass-through + severity/copy mapping).
 */
fun buildSmartInsights(state: InsightsState): List<InsightUiModel> {
    if (state.previousRecord == null) {
        return listOf(
            InsightUiModel(
                title = InsightsStrings.smartInsightsFirstStatementTitle,
                explanation = InsightsStrings.smartInsightsFirstStatementBody,
                severity = InsightSeverity.INFO,
            ),
        )
    }

    val anomalyCards =
        InsightPrioritizationEngine.prioritize(state.engineResult.anomalies).map { it.toInsightUiModel() }
    val opportunityCards = state.optimizationResult.opportunities.map { it.toInsightUiModel() }
    return anomalyCards + opportunityCards
}

private fun Anomaly.toInsightUiModel(): InsightUiModel =
    InsightUiModel(
        title = anomalyCategoryLabel(type),
        explanation = description,
        severity = AnomalySeverityMapper.severityOf(type),
        amountLabel = if (amount > 0.0) formatCurrency(amount) else null,
        actionLabel = anomalyActionLabel(type),
        actionTarget = anomalyActionTarget(type),
    )

private fun Opportunity.toInsightUiModel(): InsightUiModel =
    InsightUiModel(
        title = title,
        explanation = action,
        severity = InsightSeverity.OPPORTUNITY,
        amountLabel = if (estTaxSaved > 0.0) formatCurrency(estTaxSaved) else null,
        actionLabel = action,
        actionTarget = Screen.TaxPlanning,
    )

private fun anomalyActionLabel(type: String): String? =
    when (type) {
        "MISSING_ALLOWANCE" -> InsightsStrings.wellnessImproveMissingAllowance
        "SALARY_LOSS" -> InsightsStrings.wellnessImproveSalaryLoss
        "DEDUCTION_SPIKE" -> InsightsStrings.wellnessImproveDeductionSpike
        "TPTA_ENTITLEMENT" -> InsightsStrings.wellnessImproveTptaEntitlement
        "DSOP_COMPLIANCE" -> InsightsStrings.wellnessDsopNonCompliance
        "RENT_RECOVERY_RISK" -> InsightsStrings.smartInsightsActionRentRecoveryRisk
        "TAX_PROJECTION" -> InsightsStrings.smartInsightsActionTaxProjection
        "DEBIT_RECOVERY" -> InsightsStrings.smartInsightsActionDebitRecovery
        else -> null
    }

private fun anomalyActionTarget(type: String): Screen? =
    when (type) {
        in REPRESENTATION_DRAFT_TYPES -> Screen.Representation
        "DSOP_COMPLIANCE" -> Screen.RetirementPlanning
        "DEDUCTION_SPIKE", "TAX_PROJECTION" -> Screen.TaxPlanning
        else -> null
    }
