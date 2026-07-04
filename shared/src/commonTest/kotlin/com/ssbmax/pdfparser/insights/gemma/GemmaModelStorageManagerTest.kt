package com.ssbmax.pdfparser.insights.gemma

import kotlin.test.Test
import kotlin.test.assertFalse

class GemmaModelStorageManagerTest {
    @Test
    fun verifyModelFileReturnsNotReadyWhenPathResolvesButFileIsMissing() {
        val manager = GemmaModelStorageManager()

        // A resolvable filename that (almost certainly) does not exist on disk in a test
        // environment must never be reported as ready, regardless of device RAM/storage
        // capability — this is the check that was previously entirely absent.
        val info = manager.verifyModelFile("nonexistent-gemma-model-${Long.MAX_VALUE}.task")

        assertFalse(info.isReady)
    }
}
