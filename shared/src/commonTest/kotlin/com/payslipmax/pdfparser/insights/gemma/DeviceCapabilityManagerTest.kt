package com.payslipmax.pdfparser.insights.gemma

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceCapabilityManagerTest {
    @Test
    fun testLowRamDevice_bypassesLocalGemma100PercentOffline() {
        val lowRamMb = 3000L // 3GB RAM
        val canRun = DeviceCapabilityManager.isRamSufficientForGemma(lowRamMb)
        assertFalse(canRun)
    }

    @Test
    fun testHighRamDevice_enablesLocalGemma() {
        val highRamMb = 6000L // 6GB RAM
        val canRun = DeviceCapabilityManager.isRamSufficientForGemma(highRamMb)
        assertTrue(canRun)
    }

    @Test
    fun testBoundaryRamDevice_enablesLocalGemmaAt4Gb() {
        val boundaryRamMb = 4096L // 4GB RAM
        val canRun = DeviceCapabilityManager.isRamSufficientForGemma(boundaryRamMb)
        assertTrue(canRun)
    }
}
