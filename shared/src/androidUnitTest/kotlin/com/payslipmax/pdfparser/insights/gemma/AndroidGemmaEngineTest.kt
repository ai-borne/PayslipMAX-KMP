package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidGemmaEngineTest {
    @Test
    fun testAndroidGemmaEngineInitializationWithEmptyPathFails() =
        runTest {
            val config = GemmaEngineConfig(modelPath = "")
            val engine = GemmaEngine(config)
            assertFalse(engine.isInitialized)
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isFailure)
            engine.close()
        }

    @Test
    fun testAndroidGemmaEngineGracefulHandlingForNonExistentFile() =
        runTest {
            val config = GemmaEngineConfig(modelPath = "/tmp/non_existent_gemma_model.litertlm")
            val engine = GemmaEngine(config)
            assertFalse(engine.isInitialized)
            val result = engine.generateResponse("Test prompt")
            assertTrue(result.isFailure)
            engine.close()
        }
}
