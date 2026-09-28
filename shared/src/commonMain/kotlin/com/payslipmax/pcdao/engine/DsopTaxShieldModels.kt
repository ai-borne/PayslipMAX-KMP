package com.payslipmax.pcdao.engine

import com.payslipmax.pcdao.model.AuditDiscrepancy
import kotlinx.serialization.Serializable

@Serializable
data class DsopTaxShieldRequest(
    val monthlySubscription: Double = 0.0,
    val annualSubscription: Double = monthlySubscription * 12,
    val annualTaxExemptLimit: Double = 500000.0,
    val interestRate: Double = 0.071,
    val marginalTaxRate: Double = 0.312,
    val monthsToRetirement: Int? = null,
    val daysToRetirement: Int? = null,
    val retirementDate: String? = null,
    val asOfDate: String? = null,
)

@Serializable
data class DsopTaxShieldResult(
    val monthlySubscription: Double,
    val annualProjectedSubscription: Double,
    val taxExemptLimit: Double,
    val excessContribution: Double,
    val taxableInterest: Double,
    val estimatedTaxDrag: Double,
    val isRetirementStoppageViolated: Boolean,
    val daysToRetirement: Int?,
    val discrepancies: List<AuditDiscrepancy>,
) {
    val hasTaxExposure: Boolean get() = excessContribution > 0.0
    val hasRetirementAlarm: Boolean get() = isRetirementStoppageViolated
}
