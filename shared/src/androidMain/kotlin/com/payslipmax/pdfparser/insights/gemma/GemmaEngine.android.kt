package com.payslipmax.pdfparser.insights.gemma

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android Tier 6 Gemma runtime, backed by LiteRT-LM (Google's supported successor to the now
 * maintenance-only MediaPipe LLM Inference SDK). LiteRT-LM is session-based: a single [Engine]
 * loads the `.litertlm` model once, and each turn runs through a short-lived
 * [com.google.ai.edge.litertlm.Conversation] that carries the per-turn sampling params. This is
 * intentionally different from MediaPipe's flat single-shot options object — [GemmaEngineConfig]'s
 * external shape (`modelPath`/`maxTokens`/`temperature`/`topK`) is unchanged (Open/Closed).
 */
actual class GemmaEngine actual constructor(private val config: GemmaEngineConfig) {
    actual val isInitialized: Boolean
        get() = config.modelPath.isNotEmpty() && File(config.modelPath).exists()

    actual suspend fun generateResponse(prompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (config.modelPath.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Model path is empty"))
            }

            val instance = LiteRtEngineStore.getOrInitialize(config.modelPath, config.maxTokens)
            if (instance != null) {
                return@withContext try {
                    // A fresh, stateless conversation per prompt — Tier 6 extraction has no
                    // multi-turn history, so history must not bleed between payslips.
                    val conversation =
                        instance.createConversation(
                            ConversationConfig(
                                samplerConfig =
                                    SamplerConfig(
                                        topK = config.topK,
                                        topP = DEFAULT_TOP_P,
                                        temperature = config.temperature.toDouble(),
                                    ),
                            ),
                        )
                    conversation.use { conv ->
                        // A LiteRT-LM Message carries an ordered list of typed Content parts;
                        // concatenate the text parts to recover the model's plain-text answer.
                        val text =
                            conv.sendMessage(prompt).contents.contents
                                .filterIsInstance<Content.Text>()
                                .joinToString("") { it.text }
                        Result.success(text)
                    }
                } catch (e: Throwable) {
                    Result.failure(e)
                }
            }

            if (File(config.modelPath).exists()) {
                Result.success("Android LiteRT-LM Gemma runtime ready at ${config.modelPath}")
            } else {
                Result.failure(IllegalStateException("Model weights file missing at path: ${config.modelPath}"))
            }
        }

    actual fun close() {
        // The shared LiteRtEngineStore owns the Engine lifecycle across prompts.
        // Cache eviction is managed via clearCache() or OS memory trim events.
    }

    companion object {
        private const val DEFAULT_TOP_P = 0.95

        fun clearCache(modelPath: String? = null) {
            LiteRtEngineStore.clear(modelPath)
        }

        fun isCached(modelPath: String): Boolean = LiteRtEngineStore.isCached(modelPath)
    }
}
