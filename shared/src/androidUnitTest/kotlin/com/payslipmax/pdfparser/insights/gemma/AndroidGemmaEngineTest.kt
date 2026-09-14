package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidGemmaEngineTest {
    @org.junit.After
    fun tearDown() {
        GemmaEngine.clearCache()
    }

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

    @Test
    fun testAndroidGemmaEngineDoesNotEagerlyLoadOnConstruction() =
        runTest {
            val fakePath = "/tmp/fake_model_path.litertlm"
            val config = GemmaEngineConfig(modelPath = fakePath)
            val engine = GemmaEngine(config)

            // Creating the GemmaEngine instance must NOT load or cache the engine in native memory
            assertFalse(GemmaEngine.isCached(fakePath), "Engine must not be eagerly cached on construction")
            assertFalse(LiteRtEngineStore.isCached(fakePath), "LiteRtEngineStore must not cache on construction")
            engine.close()
        }

    @Test
    fun testAndroidGemmaEngineClearCacheReleasesEngine() =
        runTest {
            val fakePath = "/tmp/fake_model_to_clear.litertlm"
            GemmaEngine.clearCache(fakePath)
            assertFalse(GemmaEngine.isCached(fakePath))

            GemmaEngine.clearCache()
            assertFalse(GemmaEngine.isCached(fakePath))
        }
}
