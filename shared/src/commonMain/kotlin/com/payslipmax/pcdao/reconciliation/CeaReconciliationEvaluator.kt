package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.model.LedgerDifferenceItem
import com.payslipmax.pcdao.timeline.VaultMonthGroupMapper
import com.payslipmax.pdfparser.domain.ParsedPayslip

/**
 * Handles annual Children Education Allowance (CEA) and Hostel Subsidy reconciliation.
 * Prevents premature false alarms by evaluating the entire Financial Year lifecycle.
 */
class CeaReconciliationEvaluator(
    private val creditMatcher: ReconciledCreditMatcher = ReconciledCreditMatcher(),
) {
    fun evaluateCeaEntitlement(
        payslip: ParsedPayslip,
        ent: ResolvedEntitlement,
        allPayslips: List<ParsedPayslip>,
        lineItems: MutableList<LedgerDifferenceItem>,
        discrepancies: MutableList<AuditDiscrepancy>,
    ) {
        val currentMonthCredit = creditMatcher.getCreditedAmount(payslip, ent.allowanceKey)
        if (currentMonthCredit > 0.0) return

        val fyLabel = VaultMonthGroupMapper.getFinancialYearLabel(payslip.year, payslip.monthNum)
        val fyPayslips =
            allPayslips.filter {
                VaultMonthGroupMapper.getFinancialYearLabel(it.year, it.monthNum) == fyLabel
            }
        val creditedInFy = fyPayslips.any { creditMatcher.hasCeaCredit(it) }
        if (creditedInFy) return

        val isCompletedFy =
            allPayslips.size >= 12 || fyPayslips.size == 12 ||
                (payslip.monthNum == 3 && (allPayslips.isEmpty() || fyPayslips.isNotEmpty()))
        if (isCompletedFy) {
            emitCompletedFyDiscrepancy(fyLabel, fyPayslips.size, ent, lineItems, discrepancies)
        } else {
            emitOngoingSessionNudge(fyLabel, ent, discrepancies)
        }
    }

    private fun emitCompletedFyDiscrepancy(
        fyLabel: String,
        auditedCount: Int,
        ent: ResolvedEntitlement,
        lineItems: MutableList<LedgerDifferenceItem>,
        discrepancies: MutableList<AuditDiscrepancy>,
    ) {
        val netDiffAnnual = ent.entitledAnnual
        lineItems.add(
            LedgerDifferenceItem(
                allowanceKey = ent.allowanceKey,
                allowanceName = ent.allowanceName,
                creditedAmount = 0.0,
                entitledAmount = ent.entitledMonthly,
                netDifference = ent.entitledMonthly,
                authorityRef = ent.statutoryAuthority,
            ),
        )
        discrepancies.add(
            AuditDiscrepancy(
                id = "DISC_UNDERPAY_${ent.allowanceKey}",
                title = "${ent.allowanceName} Unclaimed / Lapsed Due",
                type = DiscrepancyType.UNDERPAYMENT,
                severity = DiscrepancySeverity.WARNING,
                monthlyImpact = ent.entitledMonthly,
                annualImpact = netDiffAnnual,
                drawnAmount = 0.0,
                entitledAmount = ent.entitledMonthly,
                netDue = netDiffAnnual,
                authority = ent.statutoryAuthority,
                explanation = "No CEA claim found across $fyLabel ($auditedCount months audited). Post-academic claim may be unclaimed.",
                recommendedAction = "Submit contingent bill along with school bonafide certificate to PCDA(O) Pune.",
                relevantRuleId = ent.relevantRuleId,
            ),
        )
    }

    private fun emitOngoingSessionNudge(
        fyLabel: String,
        ent: ResolvedEntitlement,
        discrepancies: MutableList<AuditDiscrepancy>,
    ) {
        discrepancies.add(
            AuditDiscrepancy(
                id = "DISC_NUDGE_${ent.allowanceKey}",
                title = "${ent.allowanceName} (Annual Claim Advisory)",
                type = DiscrepancyType.UNDERPAYMENT,
                severity = DiscrepancySeverity.INFO,
                monthlyImpact = 0.0,
                annualImpact = 0.0,
                drawnAmount = 0.0,
                entitledAmount = ent.entitledAnnual,
                netDue = ent.entitledAnnual,
                authority = ent.statutoryAuthority,
                explanation = "CEA is claimed annually post-academic session (after March 31). No claim recorded yet in $fyLabel. Ensure claim is submitted via Unit DO II.",
                recommendedAction = "Verify if claim was submitted. If not, submit school bonafide certificate after session ends.",
                relevantRuleId = ent.relevantRuleId,
            ),
        )
    }
}
