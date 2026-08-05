@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.payslipmax.pdfparser.insights.gemma

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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosGemmaEngineTest {
    private val tempModelPath = NSTemporaryDirectory() + "fake_ios_gemma_model.litertlm"

    @AfterTest
    fun cleanup() {
        NSFileManager.defaultManager.removeItemAtPath(tempModelPath, error = null)
        // Reset the companion-object delegate so a delegate registered by one test can never leak
        // into the next (the same @AfterTest discipline the temp file already gets).
        GemmaEngine.inferenceDelegate = null
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
            val config = GemmaEngineConfig(modelPath = "/tmp/non_existent_ios_gemma_model.litertlm")
            val engine = GemmaEngine(config)
            assertFalse(engine.isInitialized)
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isFailure)
            engine.close()
        }

    @Test
    fun testGenerateResponseFailsLoudlyWhenNoInferenceRuntimeRegistered() =
        runTest {
            // A model file exists, but no Swift bridge is registered. It must fail loudly rather than
            // return a fake success string — otherwise garbage would flow into the reconciled maps.
            writeTempFile(tempModelPath, "not a real gemma model".encodeToByteArray())
            GemmaEngine.inferenceDelegate = null
            val config = GemmaEngineConfig(modelPath = tempModelPath)
            val engine = GemmaEngine(config)
            assertFalse(engine.isInitialized, "isInitialized must be false when no inferenceDelegate is registered")
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isFailure, "generateResponse must fail when no inference runtime is wired")
            engine.close()
        }

    @Test
    fun testGenerateResponseSucceedsWhenDelegateReturnsText() =
        runTest {
            // The regression guard that used to assert "always fails" flips here: with a registered
            // inference delegate (simulating the Swift LiteRT-LM bridge), real inference must succeed.
            writeTempFile(tempModelPath, "not a real gemma model".encodeToByteArray())
            var seenModelPath: String? = null
            GemmaEngine.inferenceDelegate = { modelPath, prompt, completion ->
                seenModelPath = modelPath
                completion("PARSED::$prompt", null)
            }
            val config = GemmaEngineConfig(modelPath = tempModelPath)
            val engine = GemmaEngine(config)
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isSuccess, "generateResponse must succeed when the delegate returns text")
            assertEquals("PARSED::Test prompt", result.getOrNull())
            assertEquals(tempModelPath, seenModelPath, "the active-slot model path must reach the bridge")
            engine.close()
        }

    @Test
    fun testGenerateResponseFailsWhenDelegateReportsError() =
        runTest {
            // The bridge signals failure via a non-null error string; that must surface as a failed Result.
            writeTempFile(tempModelPath, "not a real gemma model".encodeToByteArray())
            GemmaEngine.inferenceDelegate = { _, _, completion ->
                completion(null, "native inference failed")
            }
            val config = GemmaEngineConfig(modelPath = tempModelPath)
            val engine = GemmaEngine(config)
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isFailure, "a delegate error must surface as a failed Result")
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
