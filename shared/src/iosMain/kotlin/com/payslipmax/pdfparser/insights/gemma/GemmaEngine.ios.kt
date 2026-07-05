package com.payslipmax.pdfparser.insights.gemma

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS Tier 6 Gemma runtime, backed by LiteRT-LM (Google's supported successor to the now
 * maintenance-only MediaPipe LLM Inference SDK). Kotlin/Native cinterop cannot bind LiteRT-LM's
 * pure-Swift package (it ships no Objective-C headers), so the actual model load + inference runs
 * in a Swift bridge (`GemmaInferenceBridge.swift`) registered as [inferenceDelegate] at app
 * startup. This is the same "Swift-only capability behind a plain closure" pattern
 * [com.payslipmax.pdfparser.auth.AuthTokenProvider] uses for Firebase Auth — see
 * docs/AI_INSIGHTS_PIPELINE.md.
 */
@OptIn(ExperimentalForeignApi::class)
actual class GemmaEngine actual constructor(private val config: GemmaEngineConfig) {
    actual val isInitialized: Boolean
        get() = config.modelPath.isNotEmpty() && isFileExists(config.modelPath)

    actual suspend fun generateResponse(prompt: String): Result<String> {
        val delegate =
            inferenceDelegate
                ?: return Result.failure(
                    IllegalStateException(
                        "Gemma/Tier 6 offline fallback is unavailable on iOS: no inference runtime is " +
                            "registered (GemmaEngine.inferenceDelegate is null).",
                    ),
                )
        if (config.modelPath.isEmpty() || !isFileExists(config.modelPath)) {
            return Result.failure(
                IllegalStateException("Model weights file missing at path: ${config.modelPath}"),
            )
        }
        // The Swift side invokes the completion exactly once with (resultText, null) on success or
        // (null, errorMessage) on failure — a Swift-friendly two-nullable shape, since constructing a
        // Kotlin Result across the interop boundary from Swift is impractical.
        return suspendCoroutine { continuation ->
            delegate(config.modelPath, prompt) { result, error ->
                if (result != null) {
                    continuation.resume(Result.success(result))
                } else {
                    continuation.resume(
                        Result.failure(RuntimeException(error ?: "iOS Gemma inference failed")),
                    )
                }
            }
        }
    }

    actual fun close() {
        // The Swift bridge owns the LiteRT-LM Engine lifecycle (it caches one engine per model path
        // across prompts), so there is no per-instance native handle to release here.
    }

    private fun isFileExists(path: String): Boolean {
        return try {
            fileExistsAt(path)
        } catch (e: Throwable) {
            false
        }
    }

    companion object {
        /**
         * Registered by the Swift entry point (`iOSApp.swift`). Given `(modelPath, prompt, completion)`,
         * the Swift bridge loads/caches a LiteRT-LM `Engine` for `modelPath`, runs inference, and calls
         * `completion(resultText, errorMessage)` with exactly one of the two arguments non-null.
         *
         * Passing `modelPath` (rather than the plan's 2-arg `(prompt, completion)`) keeps the
         * "which file/version is active" decision on the Kotlin [GemmaModelStorageManager] SSOT
         * instead of duplicating slot resolution in Swift.
         */
        var inferenceDelegate: ((String, String, (String?, String?) -> Unit) -> Unit)? = null
    }
}
