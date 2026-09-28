package com.payslipmax.pcdao.model

import kotlinx.serialization.Serializable

@Serializable
enum class DiscrepancyType {
    UNDERPAYMENT,
    RECOVERY_HAZARD,
    FORFEITURE_RISK,
    TAX_EXPOSURE,
}

@Serializable
enum class DiscrepancySeverity {
    CRITICAL,
    WARNING,
    INFO,
}

@Serializable
data class AuditDiscrepancy(
    val id: String,
    val title: String,
    val type: DiscrepancyType,
    val severity: DiscrepancySeverity,
    val monthlyImpact: Double = 0.0,
    val annualImpact: Double = 0.0,
    val drawnAmount: Double = 0.0,
    val entitledAmount: Double = 0.0,
    val netDue: Double = 0.0,
    val authority: String,
    val explanation: String,
    val recommendedAction: String,
    val relevantRuleId: String? = null,
)

@Serializable
data class MonetaryImpact(
    val principal: Double,
    val penalInterestRate: Double = 0.18,
    val interestAmount: Double = 0.0,
    val totalExposure: Double = principal + interestAmount,
)

@Serializable
data class LedgerDifferenceItem(
    val allowanceKey: String,
    val allowanceName: String,
    val creditedAmount: Double,
    val entitledAmount: Double,
    val netDifference: Double = entitledAmount - creditedAmount,
    val authorityRef: String,
)
