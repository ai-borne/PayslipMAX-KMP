package com.ssbmax.pdfparser.insights

class GeminiCloudProvider(
    private val proxyService: GeminiProxyService
) : AIInsightProvider {
    override suspend fun generateInsights(payload: PromptPayload): Result<String> {
        return proxyService.getNarrativeInsights(
            sanitizedPayslip = payload.sanitizedPayslip,
            engineResult = payload.engineResult,
            history = payload.history,
            authToken = payload.authToken
        )
    }
}
