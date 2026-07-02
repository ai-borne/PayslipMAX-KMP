package com.ssbmax.pdfparser.insights.gemma

expect class GemmaEngine(config: GemmaEngineConfig) {
    val isInitialized: Boolean

    suspend fun generateResponse(prompt: String): Result<String>

    fun close()
}
