package com.ssbmax.pdfparser.insights.gemma

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GemmaBenchmarkTest {
    @Test
    fun testStorageManagerVerification() {
        val manager = GemmaModelStorageManager()
        val emptyInfo = manager.verifyModelFile("")
        assertFalse(emptyInfo.isReady)

        assertEquals("gemma-active.litertlm", manager.getRecommendedModelFileName())
    }

    @Test
    fun testBenchmarkHarnessTracksExecution() =
        runTest {
            val harness = GemmaBenchmarkHarness()
            val config = GemmaEngineConfig(modelPath = "test/path.litertlm")
            val engine = MockGemmaEngine(config, mockResponse = "Output tokens response for benchmarking test")
            val result = harness.benchmarkMockEngine(engine)

            assertTrue(result.isSuccess)
            assertTrue(result.generationTimeMs >= 0)
            assertTrue(result.estimatedTokensPerSec >= 0.0)
        }
}
