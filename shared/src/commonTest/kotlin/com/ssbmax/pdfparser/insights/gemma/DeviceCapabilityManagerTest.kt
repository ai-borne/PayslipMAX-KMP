package com.ssbmax.pdfparser.insights.gemma

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceCapabilityManagerTest {
    @Test
    fun testDeviceCapabilitySupportedWithTrivialRequirements() {
        val manager = DeviceCapabilityManager()
        val status = manager.checkGemmaSupport(requiredRamMb = 1L, requiredStorageMb = 1L)
        assertEquals(GemmaSupportStatus.Supported, status)
    }

    @Test
    fun testDeviceCapabilityInsufficientRamWithImpossibleRequirement() {
        val manager = DeviceCapabilityManager()
        val status = manager.checkGemmaSupport(requiredRamMb = Long.MAX_VALUE, requiredStorageMb = 1L)
        assertTrue(status is GemmaSupportStatus.InsufficientRam)
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
