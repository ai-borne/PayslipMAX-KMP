package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.AnomalyTierMap
import com.payslipmax.pdfparser.insights.PayAuditFindingTypes
import com.payslipmax.pdfparser.insights.REPRESENTATION_DRAFT_TYPES
import com.payslipmax.pdfparser.insights.isProven

/**
 * Splits the engine's Pay Audit findings by what the user should do about each: an unresolved
 * ([Anomaly.isPending]) one waits for a later payslip, an [AnomalyTierMap.ARREARS_AUDIT] one is a
 * "Verified … match exactly" arrears row (DaArrearsAuditor only ever emits it on a match), the rest are issues.
 */
internal fun classifyPayAuditFindings(anomalies: List<Anomaly>): PayAuditMonthFindings {
    val findings = anomalies.filter { it.type in PayAuditFindingTypes.TYPES }
    val (waiting, resolved) = findings.partition { it.isPending }
    val (verified, issues) = resolved.partition { it.type == AnomalyTierMap.ARREARS_AUDIT }
    return PayAuditMonthFindings(issues = issues, waiting = waiting, verified = verified)
}

/** Only a proven finding of a letter-eligible type gets a "Draft letter" action (same rule as the draft generator). */
fun Anomaly.canDraftLetter(): Boolean = type in REPRESENTATION_DRAFT_TYPES && isProven() && !isPending

internal fun payAuditLinesChecked(payslip: ParsedPayslip): Int = getCreditsList(payslip).size + getDebitsList(payslip).size

internal fun buildPayAuditVerdict(
    findings: PayAuditMonthFindings,
    hasAccess: Boolean,
    linesChecked: Int,
): PayAuditVerdict {
    val labels = findings.issues.map { payLineLabel(it.field) }.distinct()
    return when {
        findings.issues.isNotEmpty() && !hasAccess -> PayAuditVerdict.LockedIssue(findings.issues.size, labels)
        findings.issues.isNotEmpty() ->
            PayAuditVerdict.Issue(findings.issues.size, labels, findings.issues.singleOrNull()?.amount, findings.issues.any { it.canDraftLetter() })
        findings.waiting.isNotEmpty() -> PayAuditVerdict.Waiting(findings.waiting.size)
        else -> PayAuditVerdict.Clean(linesChecked, findings.verified.size)
    }
}
