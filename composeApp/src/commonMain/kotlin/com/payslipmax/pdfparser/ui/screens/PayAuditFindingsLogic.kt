package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.PayAuditFindingTypes

/**
 * Display partition for the Pay Audit screen's findings, gated by
 * [com.payslipmax.pdfparser.subscription.FeatureGate.ANOMALY_DETECTION] (docs/Plan/09_PayAudit_PhasePlan.md
 * Phase 4: "Free tier: timeline and finding count. Premium: details."). Mirrors
 * [partitionAdvancedAnomalies], scoped to [PayAuditFindingTypes.TYPES] only.
 */
data class PayAuditFindingsDisplay(
    val unlocked: List<Anomaly>,
    val lockedCount: Int,
    val lockedLabels: List<String>,
) {
    val totalCount: Int get() = unlocked.size + lockedCount
    val isLocked: Boolean get() = lockedCount > 0
}

fun partitionPayAuditFindings(
    anomalies: List<Anomaly>,
    hasAnomalyDetection: Boolean,
): PayAuditFindingsDisplay {
    val findings = anomalies.filter { it.type in PayAuditFindingTypes.TYPES }
    return if (hasAnomalyDetection) {
        PayAuditFindingsDisplay(unlocked = findings, lockedCount = 0, lockedLabels = emptyList())
    } else {
        PayAuditFindingsDisplay(
            unlocked = emptyList(),
            lockedCount = findings.size,
            lockedLabels = findings.map { anomalyCategoryLabel(it.type) }.distinct(),
        )
    }
}
