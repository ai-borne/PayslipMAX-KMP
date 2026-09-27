package com.payslipmax.pdfparser.insights

/**
 * SSOT for which [Anomaly.type]s are Pay Audit findings (docs/Plan/09_PayAudit_PhasePlan.md Phase 4):
 * the [ServiceTimeline][com.payslipmax.pdfparser.insights.timeline.ServiceTimeline]-based auditors
 * ([MissingAllowanceAuditor], [TptaEntitlementAuditor], [IncrementAuditor], [MspAuditor]) plus
 * [DaArrearsAuditor] — distinct from the other PRO auditors ([DsopComplianceAuditor],
 * [TaxProjectionAuditor], [MarriedQuartersRiskAuditor], [UnexpectedDebitAuditor]), which are not part
 * of the Pay Audit screen.
 */
object PayAuditFindingTypes {
    val TYPES: Set<String> =
        setOf(
            AnomalyTierMap.MISSING_ALLOWANCE,
            AnomalyTierMap.TPTA_ENTITLEMENT,
            AnomalyTierMap.ARREARS_AUDIT,
            AnomalyTierMap.INCREMENT_MISSED,
            AnomalyTierMap.MSP_SHORTFALL,
        )
}
