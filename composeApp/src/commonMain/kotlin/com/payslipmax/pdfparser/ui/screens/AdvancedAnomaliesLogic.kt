package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.AnomalyTier
import com.payslipmax.pdfparser.insights.AnomalyTierMap
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/**
 * Display partition for the advanced (PRO) anomaly surface, gated by
 * [com.payslipmax.pdfparser.subscription.FeatureGate.ANOMALY_DETECTION] (D6).
 *
 * FREE-tier anomalies ([AnomalyTier.FREE]) are intentionally not carried here — they stay free via
 * the health score and its drivers. This surface is purely about the high-value PRO auditors:
 * shown in full when unlocked, or as a type/count-only teaser (no amounts, no descriptions) when not.
 */
data class AdvancedAnomalyDisplay(
    val unlocked: List<Anomaly>,
    val lockedCount: Int,
    val lockedLabels: List<String>,
) {
    val hasContent: Boolean get() = unlocked.isNotEmpty() || lockedCount > 0
    val isLocked: Boolean get() = lockedCount > 0
}

fun partitionAdvancedAnomalies(
    anomalies: List<Anomaly>,
    hasAnomalyDetection: Boolean,
): AdvancedAnomalyDisplay {
    val pro = anomalies.filter { AnomalyTierMap.tierFor(it.type) == AnomalyTier.PRO }
    return if (hasAnomalyDetection) {
        AdvancedAnomalyDisplay(unlocked = pro, lockedCount = 0, lockedLabels = emptyList())
    } else {
        AdvancedAnomalyDisplay(
            unlocked = emptyList(),
            lockedCount = pro.size,
            lockedLabels = pro.map { anomalyCategoryLabel(it.type) }.distinct(),
        )
    }
}

/** Human-readable category name for an [Anomaly.type] (type visible even when detail is locked). */
fun anomalyCategoryLabel(type: String): String =
    when (type) {
        AnomalyTierMap.SALARY_LOSS -> InsightsStrings.anomalyLabelSalaryLoss
        AnomalyTierMap.DEDUCTION_SPIKE -> InsightsStrings.anomalyLabelDeductionSpike
        AnomalyTierMap.MISSING_ALLOWANCE -> InsightsStrings.anomalyLabelMissingAllowance
        AnomalyTierMap.TPTA_ENTITLEMENT -> InsightsStrings.anomalyLabelTptaEntitlement
        AnomalyTierMap.ARREARS_AUDIT -> InsightsStrings.anomalyLabelArrearsAudit
        AnomalyTierMap.DSOP_COMPLIANCE -> InsightsStrings.anomalyLabelDsopCompliance
        AnomalyTierMap.DSOP_MILESTONE -> InsightsStrings.anomalyLabelDsopMilestone
        AnomalyTierMap.TAX_PROJECTION -> InsightsStrings.anomalyLabelTaxProjection
        AnomalyTierMap.RENT_RECOVERY_RISK -> InsightsStrings.anomalyLabelRentRecoveryRisk
        AnomalyTierMap.DEBIT_RECOVERY -> InsightsStrings.anomalyLabelDebitRecovery
        else -> InsightsStrings.anomalyLabelUnknown
    }
