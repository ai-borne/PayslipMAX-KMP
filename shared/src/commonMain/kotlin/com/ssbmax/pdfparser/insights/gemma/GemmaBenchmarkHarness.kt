package com.ssbmax.pdfparser.insights.gemma

import kotlin.time.TimeSource

data class BenchmarkResult(
    val generationTimeMs: Long,
    val outputLength: Int,
    val estimatedTokensPerSec: Double,
    val isSuccess: Boolean,
    val error: String? = null,
)

class GemmaBenchmarkHarness {
    suspend fun benchmarkMockEngine(
        engine: MockGemmaEngine,
        prompt: String = "Benchmark payslip extraction prompt for officer CDA-12345",
    ): BenchmarkResult {
        val timeSource = TimeSource.Monotonic
        val markStart = timeSource.markNow()
        val responseResult = engine.generateResponse(prompt)
        val duration = markStart.elapsedNow()
        val durationMs = kotlin.math.max(1L, duration.inWholeMilliseconds)

        return if (responseResult.isSuccess) {
            val text = responseResult.getOrThrow()
            val approxTokens = text.split("\\s+".toRegex()).size
            val tokensPerSec = (approxTokens.toDouble() / durationMs.toDouble()) * 1000.0
            BenchmarkResult(
                generationTimeMs = durationMs,
                outputLength = text.length,
                estimatedTokensPerSec = tokensPerSec,
                isSuccess = true,
            )
        } else {
            BenchmarkResult(
                generationTimeMs = durationMs,
                outputLength = 0,
                estimatedTokensPerSec = 0.0,
                isSuccess = false,
                error = responseResult.exceptionOrNull()?.message,
            )
        }
    }
}
