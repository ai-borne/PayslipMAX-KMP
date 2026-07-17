package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.AnomalySeverityMapper
import com.payslipmax.pdfparser.insights.InsightPrioritizationEngine
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.insights.Opportunity
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import kotlin.math.abs

/** [Anomaly.type]s for which [com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository]
 *  already auto-generates a representation draft — reused here as the SSOT for which anomaly cards
 *  route to the Claim-Generator screen. Internal (not private) so [RecommendedActions] can reuse the
 *  same SSOT rather than re-deriving which anomaly types are representation-eligible. */
internal val REPRESENTATION_DRAFT_TYPES = setOf("SALARY_LOSS", "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT")

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
    return anomalyCards + opportunityCards + buildFreeFindingCards(state.currentRecord, state.previousRecord)
}

/**
 * The useful signal from the retired KeyFindings/AiHighlights sections, folded in as low-priority INFO
 * cards (per the approved redesign decision) rather than deleted outright when those sections go away
 * in Phase 4. Net-pay/gross/tax MoM deltas are dropped here — [MonthlySnapshot] already shows those —
 * so only genuinely new signal is folded in, keeping this "useful", not noise.
 */
private fun buildFreeFindingCards(
    current: LedgerRecordEntity,
    previous: LedgerRecordEntity?,
): List<InsightUiModel> =
    listOfNotNull(
        largestDeductionCard(current),
        previous?.let { biggestComponentChangeCard(current, it) },
    )

private fun largestDeductionCard(current: LedgerRecordEntity): InsightUiModel? {
    val tax = current.incomeTax
    val dsop = current.dsopSubscription
    if (tax <= 0.0 && dsop <= 0.0) return null
    val explanation = if (tax >= dsop) InsightsStrings.freeFindingLargestDeductionTax else InsightsStrings.freeFindingLargestDeductionDsop
    val amount = if (tax >= dsop) tax else dsop
    return InsightUiModel(
        title = InsightsStrings.freeFindingLargestDeductionTitle,
        explanation = explanation,
        severity = InsightSeverity.INFO,
        amountLabel = formatCurrency(amount),
    )
}

private fun biggestComponentChangeCard(
    current: LedgerRecordEntity,
    previous: LedgerRecordEntity,
): InsightUiModel? {
    val changes =
        listOf(
            InsightsStrings.freeFindingComponentBasic to (current.basicPay - previous.basicPay),
            InsightsStrings.freeFindingComponentDa to (current.dearnessAllowance - previous.dearnessAllowance),
            InsightsStrings.freeFindingComponentTpta to (current.transportAllowance - previous.transportAllowance),
            InsightsStrings.freeFindingComponentHra to (current.houseRentAllowance - previous.houseRentAllowance),
        )
    val (label, diff) = changes.maxByOrNull { abs(it.second) } ?: return null
    if (abs(diff) <= 10.0) return null
    val suffix = if (diff > 0) InsightsStrings.freeFindingIncreasedSuffix else InsightsStrings.freeFindingDecreasedSuffix
    return InsightUiModel(
        title = InsightsStrings.freeFindingComponentChangeTitle,
        explanation = "$label $suffix",
        severity = InsightSeverity.INFO,
        amountLabel = formatCurrency(abs(diff)),
    )
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
        "TAX_PROJECTION" -> InsightsStrings.smartInsightsActionTaxProjection
        // RENT_RECOVERY_RISK / DEBIT_RECOVERY have no actionTarget below (not in
        // FinancialIntelligenceRepository's representation-draft-eligible set, no other screen fits)
        // so they never get an actionLabel either — a label without a clickable target is dead UI.
        else -> null
    }

private fun anomalyActionTarget(type: String): Screen? =
    when (type) {
        in REPRESENTATION_DRAFT_TYPES -> Screen.Representation
        "DSOP_COMPLIANCE" -> Screen.RetirementPlanning
        "DEDUCTION_SPIKE", "TAX_PROJECTION" -> Screen.TaxPlanning
        else -> null
    }
