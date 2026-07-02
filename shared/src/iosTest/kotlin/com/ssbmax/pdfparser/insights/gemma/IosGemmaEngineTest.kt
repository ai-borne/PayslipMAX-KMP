package com.ssbmax.pdfparser.insights.gemma

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosGemmaEngineTest {
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
}
