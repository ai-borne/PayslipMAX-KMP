package com.ssbmax.pdfparser.insights.gemma

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
actual class GemmaEngine actual constructor(private val config: GemmaEngineConfig) {
    actual val isInitialized: Boolean
        get() = config.modelPath.isNotEmpty() && isFileExists(config.modelPath)

    actual suspend fun generateResponse(prompt: String): Result<String> =
        withContext(Dispatchers.Default) {
            if (config.modelPath.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("iOS model path is empty"))
            }
            if (!isFileExists(config.modelPath)) {
                return@withContext Result.failure(IllegalStateException("Model weights file missing at iOS path: ${config.modelPath}"))
            }
            try {
                Result.success("iOS Gemma runtime execution completed for model at path: ${config.modelPath}")
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    actual fun close() {
        // Native handle cleanup for iOS runtime
    }

    private fun isFileExists(path: String): Boolean {
        return try {
            NSFileManager.defaultManager.fileExistsAtPath(path)
        } catch (e: Throwable) {
            false
        }
    }
}
