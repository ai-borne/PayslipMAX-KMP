package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.domain.*
import kotlinx.serialization.Serializable

@Serializable
data class Anomaly(
    // "SALARY_LOSS", "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "DEDUCTION_SPIKE", "DSOP_COMPLIANCE", "RENT_RECOVERY_RISK", "DEBIT_RECOVERY", "DSOP_MILESTONE", "TAX_PROJECTION"
    val type: String,
    val field: String,
    val amount: Double,
    val month: String,
    val description: String,
)

@Serializable
data class EngineResult(
    val healthScore: Int,
    val anomalies: List<Anomaly>,
    val monthlySavingRate: Double,
    val taxRatio: Double,
)

object DeterministicIntelligenceEngine {
    private val auditors =
        listOf(
            SalaryLossAuditor(),
            MissingAllowanceAuditor(),
            TptaEntitlementAuditor(),
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
        val anomalies =
            auditors.flatMap { auditor ->
                auditor.audit(current, previous, history)
            }

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
                ),
            deductions =
                Deductions(
                    dsopSubscription = dsopSubscription,
                    incomeTax = incomeTax,
                ),
            ledgerBalances = LedgerBalances(),
            summary =
                PayslipSummary(
                    grossPay = grossPay,
                    totalDeductions = dsopSubscription + incomeTax,
                    netRemittance = netPay,
                ),
            taxAndSavings = null,
        )
    }
}
