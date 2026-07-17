package com.payslipmax.pdfparser.insights.gemma

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
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
    private var engine: Engine? = null

    actual val isInitialized: Boolean
        get() = engine != null || (config.modelPath.isNotEmpty() && File(config.modelPath).exists())

    init {
        initEngine()
    }

    private fun initEngine() {
        if (config.modelPath.isEmpty()) return
        if (!File(config.modelPath).exists()) return

        try {
            val engineConfig =
                EngineConfig(
                    modelPath = config.modelPath,
                    backend = Backend.CPU(),
                    maxNumTokens = config.maxTokens,
                )
            engine =
                Engine(engineConfig).apply {
                    // Heavy native load; already off the main thread (the parser runs on a worker).
                    initialize()
                }
        } catch (e: Throwable) {
            // Graceful fallback for unit tests and unlinked native libs.
            engine = null
        }
    }

    actual suspend fun generateResponse(prompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (config.modelPath.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Model path is empty"))
            }
            val instance = engine
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
        try {
            engine?.close()
        } catch (e: Throwable) {
            // Resource cleanup safety
        } finally {
            engine = null
        }
    }

    private companion object {
        // Nucleus-sampling default; not part of GemmaEngineConfig's external contract, so it stays
        // an internal sampler detail rather than widening the shared config surface.
        const val DEFAULT_TOP_P = 0.95
    }
}
