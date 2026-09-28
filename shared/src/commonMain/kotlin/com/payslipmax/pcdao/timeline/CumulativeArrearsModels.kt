package com.payslipmax.pcdao.timeline

import kotlinx.serialization.Serializable

@Serializable
enum class MilestoneType {
    ANNUAL_INCREMENT_VERIFIED,
    ANNUAL_INCREMENT_MISSING,
    DA_REVISION_CREDITED,
    DA_ARREARS_SPIKE,
}

@Serializable
data class CareerMilestone(
    val type: MilestoneType,
    val dateStr: String,
    val monthName: String,
    val year: Int,
    val title: String,
    val description: String,
    val monetaryImpact: Double = 0.0,
    val statutoryAuthority: String = "",
    val isAlert: Boolean = false,
)

@Serializable
data class MonthArrearsBreakdown(
    val dateStr: String,
    val monthName: String,
    val year: Int,
    val basicPay: Double,
    val allowanceName: String,
    val entitledAmount: Double,
    val creditedAmount: Double,
    val arrearsDue: Double,
)

@Serializable
data class CumulativeArrearsRollup(
    val totalUnderpaidArrears: Double,
    val totalRecoveryHazard: Double,
    val auditedMonthCount: Int,
    val startMonthDateStr: String,
    val endMonthDateStr: String,
    val monthlyBreakdowns: List<MonthArrearsBreakdown> = emptyList(),
    val primaryClaimTitle: String = "",
    val summaryText: String = "",
)
