@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.insights.gemma

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosGemmaEngineTest {
    private val tempModelPath = NSTemporaryDirectory() + "fake_ios_gemma_model.task"

    @AfterTest
    fun cleanup() {
        NSFileManager.defaultManager.removeItemAtPath(tempModelPath, error = null)
    }

    @Test
    fun testIosGemmaEngineInitializationWithEmptyPathFails() =
        runTest {
            val config = GemmaEngineConfig(modelPath = "")
            val engine = GemmaEngine(config)
            assertFalse(engine.isInitialized)
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isFailure)
            engine.close()
        }

    @Test
    fun testIosGemmaEngineGracefulHandlingForNonExistentFile() =
        runTest {
            val config = GemmaEngineConfig(modelPath = "/tmp/non_existent_ios_gemma_model.task")
            val engine = GemmaEngine(config)
            assertFalse(engine.isInitialized)
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isFailure)
            engine.close()
        }

    @Test
    fun testIosGemmaEngineFailsLoudlyEvenWhenModelFileExists() =
        runTest {
            // Regression guard: generateResponse used to return a fake success string whenever the
            // model path pointed at a real file, regardless of whether any real inference ran. It must
            // now fail loudly instead, since no iOS inference runtime is actually wired up.
            writeTempFile(tempModelPath, "not a real gemma model".encodeToByteArray())
            val config = GemmaEngineConfig(modelPath = tempModelPath)
            val engine = GemmaEngine(config)
            assertTrue(engine.isInitialized, "the file exists, so isInitialized must be true")
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isFailure, "generateResponse must fail even when the model file exists")
            engine.close()
        }

    private fun writeTempFile(
        path: String,
        bytes: ByteArray,
    ) {
        bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                .writeToFile(path, atomically = true)
        }
    }
}
