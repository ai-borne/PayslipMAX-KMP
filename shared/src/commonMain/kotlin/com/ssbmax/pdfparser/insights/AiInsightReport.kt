package com.ssbmax.pdfparser.insights

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SalaryChangeItem(
    val item: String,
    // change can be "increased", "decreased", "unchanged"
    val change: String,
    val amount: Double,
)

@Serializable
data class NewDeductionItem(
    val item: String,
    val change: String,
    val amount: Double,
)

@Serializable
data class RiskAlertItem(
    val observation: String,
    val action: String,
)

@Serializable
data class OpportunityItem(
    val opportunity: String,
    val action: String,
)

@Serializable
data class AiInsightReport(
    val salaryChanges: List<SalaryChangeItem> = emptyList(),
    val missingAllowances: List<String> = emptyList(),
    val newDeductions: List<NewDeductionItem> = emptyList(),
    val riskAlerts: List<RiskAlertItem> = emptyList(),
    val opportunities: List<OpportunityItem> = emptyList(),
    val actionRequired: List<String> = emptyList(),
)

object AiInsightReportParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    fun parse(jsonStr: String): AiInsightReport? {
        return try {
            json.decodeFromString<AiInsightReport>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }
}
