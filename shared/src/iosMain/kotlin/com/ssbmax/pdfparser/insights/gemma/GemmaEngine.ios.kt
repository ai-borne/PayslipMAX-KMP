package com.ssbmax.pdfparser.insights.gemma

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalForeignApi::class)
actual class GemmaEngine actual constructor(private val config: GemmaEngineConfig) {
    actual val isInitialized: Boolean
        get() = config.modelPath.isNotEmpty() && isFileExists(config.modelPath)

    actual suspend fun generateResponse(prompt: String): Result<String> =
        withContext(Dispatchers.Default) {
            // Tier 6 Gemma inference is not implemented on iOS (no MediaPipe/Kotlin-Native artifact
            // exists — see docs/AI_INSIGHTS_PIPELINE.md). Always fail loudly rather than returning a
            // fake success string, so wiring this into PlatformPdfParser later fails fast instead of
            // silently returning garbage into the reconciled maps.
            Result.failure(
                NotImplementedError(
                    "Gemma/Tier 6 offline fallback is unavailable on iOS: no on-device inference runtime is wired up.",
                ),
            )
        }

    actual fun close() {
        // Native handle cleanup for iOS runtime
    }

    private fun isFileExists(path: String): Boolean {
        return try {
            fileExistsAt(path)
        } catch (e: Throwable) {
            false
        }
    }
}
