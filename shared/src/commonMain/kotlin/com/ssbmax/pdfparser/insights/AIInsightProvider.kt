package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.domain.ParsedPayslip
import kotlinx.serialization.Serializable

@Serializable
data class PromptPayload(
    val currentMonthRawText: String,
    val sanitizedJsonData: String,
    val historicalSummaryText: String,
    val anomaliesCount: Int,
    val sanitizedPayslip: ParsedPayslip,
    val engineResult: EngineResult,
    val history: List<LedgerRecordEntity> = emptyList(),
    val authToken: String? = null
)

interface AIInsightProvider {
    suspend fun generateInsights(payload: PromptPayload): Result<String>
}
