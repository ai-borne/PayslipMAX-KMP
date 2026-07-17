package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.insights.gemma.GemmaEngine
import com.payslipmax.pdfparser.insights.gemma.MockGemmaEngine

/**
 * Shared engine-selection and invocation logic for Tier 6 Gemma calls (fallback extraction and
 * diagnostic suggestion): prefers [mockEngine] when initialized (tests), otherwise falls back to
 * [gemmaEngine], and swallows any failure into a [Result.failure]. Shared by all Tier 6 extractors.
 */
internal object GemmaEngineInvoker {
    suspend fun generateResponse(
        prompt: String,
        gemmaEngine: GemmaEngine?,
        mockEngine: MockGemmaEngine?,
    ): Result<String> {
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
