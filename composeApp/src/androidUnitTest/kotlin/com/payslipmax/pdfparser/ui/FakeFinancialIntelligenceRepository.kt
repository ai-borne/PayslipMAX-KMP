package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.GeminiProxyService
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class FakeFinancialIntelligenceRepository(
    val fakeDao: FakePayslipDao = FakePayslipDao(),
    var cachedInsightsByMonth: MutableMap<String, String?> = mutableMapOf(),
    var narrativeResult: Result<String> = Result.success("Fake AI narrative"),
    var generateCallCount: Int = 0,
) : FinancialIntelligenceRepository(
        payslipDao = fakeDao,
        geminiProxyService =
            GeminiProxyService(
                HttpClient(MockEngine { respond("", HttpStatusCode.OK, headersOf()) }),
            ),
    ) {
    override suspend fun getCachedAiInsights(monthStr: String): String? =
        cachedInsightsByMonth[monthStr]

    override suspend fun processPayslipAndRunAnalysis(payslip: ParsedPayslip): EngineResult =
        EngineResult(
            healthScore = 80,
            anomalies = emptyList(),
            monthlySavingRate = 10.0,
            taxRatio = 4.0,
        )

    override suspend fun generateNarrativeInsights(
        payslip: ParsedPayslip,
        engineResult: EngineResult,
    ): Result<String> {
        generateCallCount++
        if (narrativeResult.isSuccess) {
            cachedInsightsByMonth[payslip.dateStr] = narrativeResult.getOrThrow()
        }
        return narrativeResult
    }
}
