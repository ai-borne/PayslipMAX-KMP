package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.insights.gemma.DeviceCapabilityManager
import com.ssbmax.pdfparser.insights.gemma.GemmaEngine
import com.ssbmax.pdfparser.insights.gemma.MockGemmaEngine

/**
 * Orchestrates Tier 6 Gemma Offline Fallback extraction for ambiguous/unmatched tokens.
 */
class GemmaFallbackExtractor(
    private val gemmaEngine: GemmaEngine? = null,
    private val mockEngine: MockGemmaEngine? = null,
    private val capabilityManager: DeviceCapabilityManager = DeviceCapabilityManager(),
) {
    suspend fun extractFallback(
        rawEarnings: Map<String, Double>,
        rawDeductions: Map<String, Double>,
    ): GemmaFallbackExtraction {
        if (rawEarnings.isEmpty() && rawDeductions.isEmpty()) {
            return GemmaFallbackExtraction()
        }

        val supportStatus = capabilityManager.checkGemmaSupport()
        if (!supportStatus.isSupported) {
            return GemmaFallbackExtraction()
        }

        val prompt = GemmaPromptBuilder.buildPrompt(rawEarnings, rawDeductions)
        val responseResult = generateResponse(prompt)

        val responseText = responseResult.getOrNull() ?: return GemmaFallbackExtraction()
        return GemmaResponseParser.parse(responseText)
    }

    private suspend fun generateResponse(prompt: String): Result<String> {
        return try {
            if (mockEngine != null && mockEngine.isInitialized) {
                mockEngine.generateResponse(prompt)
            } else if (gemmaEngine != null && gemmaEngine.isInitialized) {
                gemmaEngine.generateResponse(prompt)
            } else {
                Result.failure(IllegalStateException("Gemma engine not initialized"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
