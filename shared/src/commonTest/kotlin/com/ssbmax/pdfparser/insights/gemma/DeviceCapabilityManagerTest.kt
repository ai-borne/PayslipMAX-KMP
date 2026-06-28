package com.ssbmax.pdfparser.insights.gemma

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceCapabilityManagerTest {
    @Test
    fun testDeviceCapabilityCheckReturnsStatus() {
        val manager = DeviceCapabilityManager()
        val status = manager.checkGemmaSupport(requiredRamMb = 100L, requiredStorageMb = 100L)
        // Verify that check runs without throwing exceptions
        assertTrue(status is GemmaSupportStatus)
    }

    @Test
    fun testMockEngineStateTransitions() {
        val config = GemmaEngineConfig(modelPath = "test/path/gemma.task")
        val mockEngine = MockGemmaEngine(config)
        assertTrue(mockEngine.isInitialized)
        assertFalse(mockEngine.isClosed)

        mockEngine.close()
        assertTrue(mockEngine.isClosed)
        assertFalse(mockEngine.isInitialized)
    }
}
