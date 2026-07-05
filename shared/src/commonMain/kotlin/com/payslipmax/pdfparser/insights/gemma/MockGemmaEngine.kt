package com.payslipmax.pdfparser.insights.gemma

class MockGemmaEngine(
    private val config: GemmaEngineConfig = GemmaEngineConfig(),
    var mockResponse: String? = null,
    var shouldFail: Boolean = false,
) {
    var isClosed: Boolean = false
        private set

    val isInitialized: Boolean get() = !isClosed && config.modelPath.isNotEmpty()

    suspend fun generateResponse(prompt: String): Result<String> {
        if (isClosed) {
            return Result.failure(IllegalStateException("Engine is closed"))
        }
        if (!isInitialized) {
            return Result.failure(IllegalStateException("Gemma model weights not loaded at path: ${config.modelPath}"))
        }
        if (shouldFail) {
            return Result.failure(RuntimeException("Mock engine execution simulated failure"))
        }
        val response = mockResponse ?: "Mock Gemma generated response for prompt length: ${prompt.length}"
        return Result.success(response)
    }

    fun close() {
        isClosed = true
    }
}
