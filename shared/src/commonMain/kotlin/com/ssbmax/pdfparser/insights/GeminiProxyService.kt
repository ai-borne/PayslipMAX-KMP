package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.domain.ParsedPayslip
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class InsightProxyRequest(
    val payslip: ParsedPayslip,
    val anomalies: List<Anomaly>,
    val monthlySavingRate: Double,
    val taxRatio: Double,
    val healthScore: Int,
    val history: List<LedgerRecordEntity> = emptyList(),
)

@Serializable
data class InsightProxyResponse(
    val success: Boolean,
    val narrative: String? = null,
    val error: String? = null,
)

class GeminiProxyService(
    private val client: HttpClient,
    private val proxyUrl: String = "https://us-central1-payslipmax.cloudfunctions.net/generateInsights",
) {
    /**
     * Sends the sanitized payslip and deterministic calculations to the secure backend proxy.
     * The proxy handles injecting the developer's server-side Gemini API key and calling the AI model.
     */
    suspend fun getNarrativeInsights(
        sanitizedPayslip: ParsedPayslip,
        engineResult: EngineResult,
        history: List<LedgerRecordEntity> = emptyList(),
        authToken: String? = null,
    ): Result<String> {
        return try {
            val payload =
                InsightProxyRequest(
                    payslip = sanitizedPayslip,
                    anomalies = engineResult.anomalies,
                    monthlySavingRate = engineResult.monthlySavingRate,
                    taxRatio = engineResult.taxRatio,
                    healthScore = engineResult.healthScore,
                    history = history,
                )

            val response: InsightProxyResponse =
                client.post {
                    url(proxyUrl)
                    contentType(ContentType.Application.Json)
                    if (authToken != null) {
                        headers.append("Authorization", "Bearer $authToken")
                    }
                    setBody(payload)
                }.body()

            if (response.success && response.narrative != null) {
                Result.success(response.narrative)
            } else {
                Result.failure(Exception(response.error ?: "Failed to generate narrative insights from proxy backend."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
