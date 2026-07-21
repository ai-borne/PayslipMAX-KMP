package com.payslipmax.pdfparser.insights

import kotlinx.serialization.Serializable

@Serializable
data class TaxRulePackInfo(
    val financialYear: String,
    val assessmentYear: String,
    val version: String,
    val verifiedOn: String? = null,
    val sourceLabel: String = "CBDT Rules Offline",
    val status: String = "verified",
)

@Serializable
data class DataCoverageInfo(
    val monthsAvailable: Int,
    val monthsRemaining: Int,
    val confidenceLabel: String,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class IncomeBreakdown(
    val grossProjectedSalary: Double,
    val exemptAllowances: Double,
    val taxableSalaryBeforeDeductions: Double,
)

@Serializable
data class RegimeTaxBreakdown(
    val standardDeduction: Double,
    val allowedExemptions: Double,
    val taxableIncome: Double,
    val taxBeforeCess: Double,
    val surcharge: Double,
    val cess: Double,
    val totalTax: Double,
    val effectiveRate: Double,
)

@Serializable
data class RecommendedActionItem(
    val id: String,
    val title: String,
    val description: String,
    val estimatedTaxImpact: Double,
    val confidence: String = "medium",
)

@Serializable
data class TaxRecommendationSummary(
    val bestRegime: String,
    val estimatedSavings: Double,
    val availableActions: List<RecommendedActionItem>,
)

@Serializable
data class TdsRunwaySummary(
    val deductedYtd: Double,
    val remainingTax: Double,
    val projectedMonthlyTds: Double,
    val spikeWarning: Boolean,
)

@Serializable
data class TaxPlannerResult(
    val financialYear: String,
    val assessmentYear: String,
    val rulePack: TaxRulePackInfo,
    val dataCoverage: DataCoverageInfo,
    val income: IncomeBreakdown,
    val oldRegime: RegimeTaxBreakdown,
    val newRegime: RegimeTaxBreakdown,
    val recommendation: TaxRecommendationSummary,
    val tds: TdsRunwaySummary,
)
