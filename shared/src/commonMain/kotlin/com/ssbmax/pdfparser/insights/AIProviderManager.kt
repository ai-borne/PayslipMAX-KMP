package com.ssbmax.pdfparser.insights

class AIProviderManager(
    private val cloudProvider: AIInsightProvider,
    private val localProvider: AIInsightProvider = LocalGemmaProvider(),
    private val useLocalAi: Boolean = false,
) {
    suspend fun generateInsights(payload: PromptPayload): Result<String> {
        val provider = if (useLocalAi) localProvider else cloudProvider
        return provider.generateInsights(payload)
    }
}
