package com.payslipmax.pdfparser.insights.gemma

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File

/**
 * Thread-safe singleton store for LiteRT-LM [Engine] instances on Android.
 *
 * Parallels iOS `GemmaInferenceBridge.EngineStore`. Ensures at most one [Engine]
 * is loaded into native memory per model path, and delays all native allocations
 * until inference is actually requested.
 */
internal object LiteRtEngineStore {
    private val lock = Any()
    private val engines = mutableMapOf<String, Engine>()

    /**
     * Retrieves the cached [Engine] for [modelPath] or initializes a new one if not yet loaded.
     * Returns null if [modelPath] is empty, does not exist, or if native initialization fails.
     */
    fun getOrInitialize(
        modelPath: String,
        maxTokens: Int,
    ): Engine? {
        if (modelPath.isEmpty() || !File(modelPath).exists()) return null

        synchronized(lock) {
            engines[modelPath]?.let { return it }

            return try {
                val engineConfig =
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        maxNumTokens = maxTokens,
                    )
                val engine =
                    Engine(engineConfig).apply {
                        initialize()
                    }
                engines[modelPath] = engine
                engine
            } catch (e: Throwable) {
                null
            }
        }
    }

    /**
     * Clears and closes cached [Engine] instances.
     * If [modelPath] is supplied, only that instance is closed; otherwise all are purged.
     */
    fun clear(modelPath: String? = null) {
        synchronized(lock) {
            if (modelPath != null) {
                try {
                    engines.remove(modelPath)?.close()
                } catch (_: Throwable) {
                }
            } else {
                engines.values.forEach { engine ->
                    try {
                        engine.close()
                    } catch (_: Throwable) {
                    }
                }
                engines.clear()
            }
        }
    }

    /**
     * Checks whether an [Engine] is currently loaded in memory for [modelPath].
     */
    fun isCached(modelPath: String): Boolean {
        synchronized(lock) {
            return engines.containsKey(modelPath)
        }
    }
}
