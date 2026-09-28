package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pcdao.engine.AllowanceCollisionAuditor
import com.payslipmax.pcdao.engine.AllowanceCollisionCodes
import com.payslipmax.pcdao.engine.AllowanceCollisionRequest
import com.payslipmax.pcdao.engine.DsopTaxShieldAuditor
import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.model.LedgerDifferenceItem
import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.math.abs
import kotlin.math.round

class ShadowLedgerReconciler(
    private val ruleResolver: SituationalRuleResolver = SituationalRuleResolver(),
    private val dsopAuditor: DsopTaxShieldAuditor = DsopTaxShieldAuditor(),
    private val collisionAuditor: AllowanceCollisionAuditor = AllowanceCollisionAuditor(),
) {
    fun reconcile(
        payslip: ParsedPayslip,
        context: ActiveSituationalContext,
    ): ShadowLedgerReconciliationResult {
        val lineItems = mutableListOf<LedgerDifferenceItem>()
        val discrepancies = mutableListOf<AuditDiscrepancy>()

        val basicPay = payslip.earnings.basicPay
        val msp = payslip.earnings.militaryServicePay
        val effectiveDa = context.customDaPercent ?: context.inferredFlags.inferredDaPercent

        evaluateEntitlements(payslip, context, basicPay, msp, lineItems, discrepancies)
        evaluateRecoveryHazards(payslip, context, effectiveDa, discrepancies)
        evaluateDsopShield(payslip, context, discrepancies)
        evaluateDo2Rejection(context, discrepancies)

        val totalUnclaimed = discrepancies.filter { it.type == DiscrepancyType.UNDERPAYMENT }.sumOf { it.annualImpact }
        val totalHazard = discrepancies.filter { it.type == DiscrepancyType.RECOVERY_HAZARD }.sumOf { abs(it.netDue) }
        val criticalCount = discrepancies.count { it.severity == DiscrepancySeverity.CRITICAL }

        val summary = generateSummary(totalUnclaimed, totalHazard, criticalCount)

        return ShadowLedgerReconciliationResult(
            lineItems = lineItems,
            discrepancies = discrepancies,
            totalUnclaimedAnnual = round(totalUnclaimed * 100.0) / 100.0,
            totalRecoveryHazard = round(totalHazard * 100.0) / 100.0,
            criticalAlarmCount = criticalCount,
            summaryMessage = summary,
        )
    }

    private fun evaluateEntitlements(
        payslip: ParsedPayslip,
        context: ActiveSituationalContext,
        basicPay: Double,
        msp: Double,
        lineItems: MutableList<LedgerDifferenceItem>,
        discrepancies: MutableList<AuditDiscrepancy>,
    ) {
        val entitlements = ruleResolver.resolveAllEntitlements(context, basicPay, msp)
        for (ent in entitlements) {
            val creditedMonthly = getCreditedAmount(payslip, ent.allowanceKey)
            val netDiffMonthly = ent.entitledMonthly - creditedMonthly
            val netDiffAnnual = ent.entitledAnnual - (creditedMonthly * 12.0)

            if (netDiffAnnual > 1.0) {
                lineItems.add(
                    LedgerDifferenceItem(
                        allowanceKey = ent.allowanceKey,
                        allowanceName = ent.allowanceName,
                        creditedAmount = creditedMonthly,
                        entitledAmount = ent.entitledMonthly,
                        netDifference = netDiffMonthly,
                        authorityRef = ent.statutoryAuthority,
                    ),
                )
                discrepancies.add(
                    AuditDiscrepancy(
                        id = "DISC_UNDERPAY_${ent.allowanceKey}",
                        title = "${ent.allowanceName} Underpaid / Unclaimed",
                        type = DiscrepancyType.UNDERPAYMENT,
                        severity = if (netDiffAnnual >= 100000.0) DiscrepancySeverity.WARNING else DiscrepancySeverity.INFO,
                        monthlyImpact = netDiffMonthly,
                        annualImpact = netDiffAnnual,
                        drawnAmount = creditedMonthly,
                        entitledAmount = ent.entitledMonthly,
                        netDue = netDiffAnnual,
                        authority = ent.statutoryAuthority,
                        explanation = ent.explanation,
                        recommendedAction = "Submit supplementary claim or representation to PCDA(O) Pune.",
                        relevantRuleId = ent.relevantRuleId,
                    ),
                )
            }
        }
    }

    private fun getCreditedAmount(
        payslip: ParsedPayslip,
        key: String,
    ): Double {
        return when (key) {
            "TPTA" -> payslip.earnings.transportAllowance + payslip.earnings.transportAllowanceDa
            "HRA_SPR" -> payslip.earnings.houseRentAllowance
            "CEA", "HOSTEL_SUBSIDY" -> payslip.earnings.childrenEducationAllowance / 12.0
            else -> 0.0
        }
    }

    private fun evaluateRecoveryHazards(
        payslip: ParsedPayslip,
        context: ActiveSituationalContext,
        da: Double,
        discrepancies: MutableList<AuditDiscrepancy>,
    ) {
        val codes = mutableSetOf<String>()
        val tiles = context.activeTileIds

        if (tiles.contains(SituationalTileKeys.POST_PEACE_HIGHER)) codes.add(AllowanceCollisionCodes.TPTA_HIGHER_CITY)
        if (tiles.contains(SituationalTileKeys.POST_PEACE_OTHER)) codes.add(AllowanceCollisionCodes.TPTA_OTHER)
        if (tiles.contains(SituationalTileKeys.POST_FIELD_HAFAA)) codes.add(AllowanceCollisionCodes.HAFAA)
        if (tiles.contains(SituationalTileKeys.POST_SIACHEN)) codes.add(AllowanceCollisionCodes.SIACHEN)
        if (tiles.contains(SituationalTileKeys.HOUSE_GOVT_MQ)) codes.add(AllowanceCollisionCodes.GOVT_ACCOMM_ALLOTTED)
        if (tiles.contains(SituationalTileKeys.HOUSE_FAMILY_SPR)) codes.add(AllowanceCollisionCodes.HRA_CLAIMED)
        if (tiles.contains(SituationalTileKeys.POST_SDA_NE)) codes.add(AllowanceCollisionCodes.SDA)

        val req =
            AllowanceCollisionRequest(
                activeAllowanceCodes = codes,
                basicPay = payslip.earnings.basicPay,
                daRate = da / 100.0,
                defaultOverdrawnMonths = 6,
                payLevel = context.inferredFlags.inferredRankLevel ?: "10",
            )
        val collisionResult = collisionAuditor.audit(req)
        discrepancies.addAll(collisionResult.discrepancies)
    }

    private fun evaluateDsopShield(
        payslip: ParsedPayslip,
        context: ActiveSituationalContext,
        discrepancies: MutableList<AuditDiscrepancy>,
    ) {
        val dsopMonthly = payslip.deductions.dsopSubscription
        val monthsToRetire = context.monthsToRetirement ?: if (context.activeTileIds.contains(SituationalTileKeys.RETIRE_NEAR)) 2 else null
        val dsopResult = dsopAuditor.auditSubscription(dsopMonthly, monthsToRetire)

        if (dsopResult.excessContribution > 0) {
            discrepancies.add(
                AuditDiscrepancy(
                    id = "DISC_DSOP_TAX_EXPOSURE",
                    title = "DSOP Subscription Exceeds ₹5 Lakh Statutory Limit",
                    type = DiscrepancyType.TAX_EXPOSURE,
                    severity = DiscrepancySeverity.WARNING,
                    monthlyImpact = 0.0,
                    annualImpact = dsopResult.estimatedTaxDrag,
                    drawnAmount = dsopResult.annualProjectedSubscription,
                    entitledAmount = 500000.0,
                    netDue = dsopResult.taxableInterest,
                    authority = "Income Tax Act 1961 Section 10(11)/10(12); CBDT Circular",
                    explanation = "Annual DSOP of ₹${dsopResult.annualProjectedSubscription.toInt()} exceeds ₹5,00,000. Taxable interest is ₹${dsopResult.taxableInterest.toInt()}.",
                    recommendedAction = "Optimize DSOP monthly subscription down to ₹41,666 to avoid tax drag.",
                    relevantRuleId = "FUNDS_DSOP_001",
                ),
            )
        }

        if (dsopResult.isRetirementStoppageViolated) {
            discrepancies.add(
                AuditDiscrepancy(
                    id = "DISC_DSOP_RETIREMENT_STOPPAGE",
                    title = "Mandatory DSOP Deduction Stoppage (3 Months Prior to Retirement)",
                    type = DiscrepancyType.FORFEITURE_RISK,
                    severity = DiscrepancySeverity.CRITICAL,
                    monthlyImpact = -dsopMonthly,
                    annualImpact = -dsopMonthly * 3.0,
                    drawnAmount = dsopMonthly,
                    entitledAmount = 0.0,
                    netDue = 0.0,
                    authority = "Rule 14 DSOP Fund Rules (1933 / revised 2017)",
                    explanation = "Under Rule 14, deductions must stop 3 months before retirement to prevent settlement delay.",
                    recommendedAction = "Submit Unit DO2 to PCDA(O) stopping DSOP deductions immediately.",
                    relevantRuleId = "FUNDS_DSOP_002",
                ),
            )
        }
    }

    private fun evaluateDo2Rejection(
        context: ActiveSituationalContext,
        discrepancies: MutableList<AuditDiscrepancy>,
    ) {
        val do2Num = context.rejectedDo2Number
        if (do2Num != null) {
            discrepancies.add(
                AuditDiscrepancy(
                    id = "DISC_DO2_REJECTION",
                    title = "Rejected DO2 No. $do2Num",
                    type = DiscrepancyType.FORFEITURE_RISK,
                    severity = DiscrepancySeverity.CRITICAL,
                    authority = "PCDA(O) Pune Standing Orders, Section R-Section Audit Desk",
                    explanation = "PCDA(O) rejected Part II Order No. $do2Num dated ${context.rejectedDo2Date ?: "Recent"}. Allowance credit is blocked.",
                    recommendedAction = "Publish corrective Part II Order via Unit Adjutant.",
                    relevantRuleId = "ADMIN_DO2_001",
                ),
            )
        }
    }

    private fun generateSummary(
        unclaimed: Double,
        hazard: Double,
        alarms: Int,
    ): String {
        return "Audit complete: ₹${unclaimed.toInt()} unclaimed entitlements, ₹${hazard.toInt()} recovery hazards, $alarms critical alarms."
    }
}
