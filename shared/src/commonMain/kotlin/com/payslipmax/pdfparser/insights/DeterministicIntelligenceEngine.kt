package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.insights.timeline.ChangeExplanation
import com.payslipmax.pdfparser.insights.timeline.NextIncrementPrediction
import com.payslipmax.pdfparser.insights.timeline.NextIncrementPredictor
import com.payslipmax.pdfparser.insights.timeline.PayLineChangeExplainer
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.ServiceTimelineBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Anomaly(
    // "SALARY_LOSS", "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "DEDUCTION_SPIKE", "DSOP_COMPLIANCE", "RENT_RECOVERY_RISK",
    // "DEBIT_RECOVERY", "DSOP_MILESTONE", "TAX_PROJECTION", "ARREARS_AUDIT", "INCREMENT_MISSED", "MSP_SHORTFALL"
    val type: String,
    val field: String,
    val amount: Double,
    val month: String,
    val description: String,
    // Evidence behind a rule-based finding: the amount the rule requires, the amount the payslip shows,
    // and the verified authority (see PayAuthorities). Null where the check has no such source.
    val expected: Double? = null,
    val actual: Double? = null,
    val authority: String? = null,
    // True when the auditor could not yet rule the finding in or out (e.g. TptaAbsenceExplainer.isPendingFutureData:
    // the same-window payslip that would confirm/rule out a relocation hasn't been imported yet) — shown, not hidden,
    // but never proven (see Anomaly.isProven) and never drafts a representation letter.
    val isPending: Boolean = false,
)

@Serializable
data class EngineResult(
    val healthScore: Int,
    val anomalies: List<Anomaly>,
    val monthlySavingRate: Double,
    val taxRatio: Double,
    // The ServiceTimeline the engine builds once per run (Pay Audit, docs/Plan/09_PayAudit_PhasePlan.md
    // Phase 4) and the current month's pay-line change explanations derived from it. Neither type is
    // itself @Serializable, and EngineResult is never actually encoded/decoded today — transient with a
    // safe default keeps that annotation honest without forcing timeline/ChangeExplanation serializable too.
    @Transient val timeline: ServiceTimeline = ServiceTimeline(emptyList(), emptyList(), emptyList()),
    @Transient val changeExplanations: List<ChangeExplanation> = emptyList(),
    // Every explained transition across the whole stored history (Phase 8 P7-12), not just current's own
    // — [changeExplanations] above stays as the current-month subset the "This month" section already uses.
    @Transient val allChangeExplanations: List<ChangeExplanation> = emptyList(),
    // Predictions (Pay Audit Phase 6), both zero-input and derived from the same timeline/history the
    // engine already built above — null when there is nothing trustworthy to predict from.
    @Transient val incrementPrediction: NextIncrementPrediction? = null,
    @Transient val dsopRoom: DsopRoom? = null,
)

object DeterministicIntelligenceEngine {
    private val auditors =
        listOf(
            SalaryLossAuditor(),
            MissingAllowanceAuditor(),
            TptaEntitlementAuditor(),
            IncrementAuditor(),
            MspAuditor(),
            DaArrearsAuditor(),
            MarriedQuartersRiskAuditor(),
            UnexpectedDebitAuditor(),
            DsopComplianceAuditor(),
            TaxProjectionAuditor(),
        )

    fun analyze(
        current: LedgerRecordEntity,
        previous: LedgerRecordEntity? = null,
        history: List<LedgerRecordEntity> = emptyList(),
    ): EngineResult {
        val currPayslip = current.toParsedPayslip()
        val prevPayslip = previous?.toParsedPayslip()
        val historyPayslips = history.map { it.toParsedPayslip() }
        return analyze(currPayslip, prevPayslip, historyPayslips)
    }

    fun analyze(
        current: ParsedPayslip,
        previous: ParsedPayslip? = null,
        history: List<ParsedPayslip> = emptyList(),
    ): EngineResult {
        val timeline = ServiceTimelineBuilder.build(history + current)
        val anomalies =
            auditors.flatMap { auditor ->
                when {
                    auditor is TimelineAuditor -> auditor.audit(current, previous, timeline)
                    // Plain auditors were never rebuilt on the timeline (Phase 8 P7-20) and reason purely
                    // over raw current/previous fields, so the one timeline-derived signal that applies to
                    // all of them without a per-auditor rewrite is trust: never fire on a needsReview
                    // parse. (Excluded pay-matrix cells are a timeline-construction detail for level/stage
                    // resolution, not a general data-trust signal, so it's deliberately not part of this
                    // gate.)
                    current.needsReview || previous?.needsReview == true -> emptyList()
                    else -> auditor.audit(current, previous, history)
                }
            }
        val changeExplanations = PayLineChangeExplainer.explain(current, history, timeline)
        val allChangeExplanations = PayLineChangeExplainer.explainAll(history + current, timeline)
        val incrementPrediction = NextIncrementPredictor.predict(timeline)
        val dsopRoom = DsopRoomCalculator.calculate(current, history)

        val dsop = current.deductions.dsopSubscription
        val gross = current.summary.grossPay
        val tax = current.deductions.incomeTax

        val savingRate = if (gross > 0.0) (dsop / gross) * 100.0 else 0.0
        val taxRate = if (gross > 0.0) (tax / gross) * 100.0 else 0.0

        val score = calculateHealthScore(anomalies, savingRate)

        return EngineResult(
            healthScore = score,
            anomalies = anomalies,
            monthlySavingRate = savingRate,
            taxRatio = taxRate,
            timeline = timeline,
            changeExplanations = changeExplanations,
            allChangeExplanations = allChangeExplanations,
            incrementPrediction = incrementPrediction,
            dsopRoom = dsopRoom,
        )
    }

    private fun calculateHealthScore(
        anomalies: List<Anomaly>,
        savingRate: Double,
    ): Int {
        var score = 100

        for (anomaly in anomalies) {
            when (anomaly.type) {
                "MISSING_ALLOWANCE" -> score -= 15
                "SALARY_LOSS" -> score -= 20
                "DEDUCTION_SPIKE" -> score -= 10
                "TPTA_ENTITLEMENT" -> score -= 10
                "RENT_RECOVERY_RISK" -> score -= 15
                "DEBIT_RECOVERY" -> score -= 10
                "INCREMENT_MISSED" -> score -= 15
                "MSP_SHORTFALL" -> score -= 15
                "DSOP_COMPLIANCE" -> {
                    if (anomaly.amount > 0.0) {
                        score -= 25
                    } else {
                        // Warn only
                        score -= 5
                    }
                }
            }
        }

        if (savingRate >= 20.0) {
            score += 15
        } else if (savingRate >= 10.0) {
            score += 5
        }

        if (anomalies.isEmpty() && savingRate >= 10.0) {
            score += 10
        }

        return score.coerceIn(0, 100)
    }

    private fun LedgerRecordEntity.toParsedPayslip(): ParsedPayslip {
        return ParsedPayslip(
            file = "mock.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "",
            dateStr = dateStr,
            officer = Officer("[REDACTED]", "[REDACTED]", "[REDACTED]"),
            earnings =
                Earnings(
                    basicPay = basicPay,
                    dearnessAllowance = dearnessAllowance,
                    militaryServicePay = militaryServicePay,
                    transportAllowance = transportAllowance,
                    transportAllowanceDa = transportAllowanceDa,
                    houseRentAllowance = houseRentAllowance,
                    riskHardshipAllowance = riskHardshipAllowance,
                    fieldAllowance = fieldAllowance,
                    arrearsDa = arrearsDa,
                    arrearsTpta = arrearsTpta,
                    arrearsTptaDa = arrearsTptaDa,
                    adjTpta = adjTpta,
                    adjMsp = adjMsp,
                ),
            deductions =
                Deductions(
                    dsopSubscription = dsopSubscription,
                    incomeTax = incomeTax,
                    licenseFee = licenseFee,
                    furnitureRent = furnitureRent,
                ),
            ledgerBalances = LedgerBalances(),
            summary =
                PayslipSummary(
                    grossPay = grossPay,
                    totalDeductions = dsopSubscription + incomeTax,
                    netRemittance = netPay,
                ),
            taxAndSavings = null,
            needsReview = needsReview,
        )
    }
}
