package com.payslipmax.pdfparser.telemetry

import com.payslipmax.pdfparser.insights.gemma.BaseModelInstallState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockGemmaInstallTelemetry : BaseGemmaInstallTelemetry() {
    val loggedEvents = mutableListOf<Pair<String, Map<String, String>?>>()
    var isTelemetryEnabledState = true

    override fun onTelemetryEnabledChanged(enabled: Boolean) {
        isTelemetryEnabledState = enabled
    }

    override fun logEvent(
        name: String,
        params: Map<String, String>?,
    ) {
        loggedEvents.add(Pair(name, params))
    }
}

class GemmaInstallTelemetryTest {
    @Test
    fun testDownloadingThrottling() {
        val telemetry = MockGemmaInstallTelemetry()

        // Progress: 0.0f
        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.0f))
        assertEquals(1, telemetry.loggedEvents.size)
        assertEquals("gemma_install_progress", telemetry.loggedEvents[0].first)
        assertEquals("0%", telemetry.loggedEvents[0].second?.get("progress"))

        // Progress: 0.10f (no new event)
        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.10f))
        assertEquals(1, telemetry.loggedEvents.size)

        // Progress: 0.24f (no new event)
        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.24f))
        assertEquals(1, telemetry.loggedEvents.size)

        // Progress: 0.25f (new event for 25%)
        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.25f))
        assertEquals(2, telemetry.loggedEvents.size)
        assertEquals("gemma_install_progress", telemetry.loggedEvents[1].first)
        assertEquals("25%", telemetry.loggedEvents[1].second?.get("progress"))

        // Progress: 0.49f (no new event)
        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.49f))
        assertEquals(2, telemetry.loggedEvents.size)

        // Progress: 0.50f (new event for 50%)
        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.50f))
        assertEquals(3, telemetry.loggedEvents.size)
        assertEquals("50%", telemetry.loggedEvents[2].second?.get("progress"))

        // Progress: 0.75f (new event for 75%)
        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.75f))
        assertEquals(4, telemetry.loggedEvents.size)
        assertEquals("75%", telemetry.loggedEvents[3].second?.get("progress"))

        // Progress: 0.90f (no new event)
        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.90f))
        assertEquals(4, telemetry.loggedEvents.size)
    }

    @Test
    fun testInstalledSuccess() {
        val telemetry = MockGemmaInstallTelemetry()
        telemetry.trackInstallState(BaseModelInstallState.Installed("/path/to/model"))
        assertEquals(1, telemetry.loggedEvents.size)
        assertEquals("gemma_install_success", telemetry.loggedEvents[0].first)
    }

    @Test
    fun testFailedError() {
        val telemetry = MockGemmaInstallTelemetry()
        telemetry.trackInstallState(BaseModelInstallState.Failed("Disk full"))
        assertEquals(1, telemetry.loggedEvents.size)
        assertEquals("gemma_install_failed", telemetry.loggedEvents[0].first)
        assertEquals("Disk full", telemetry.loggedEvents[0].second?.get("error"))
    }

    @Test
    fun testDisablingTelemetry() {
        val telemetry = MockGemmaInstallTelemetry()
        telemetry.setTelemetryEnabled(false)

        telemetry.trackInstallState(BaseModelInstallState.Downloading(0.25f))
        telemetry.trackInstallState(BaseModelInstallState.Installed("/path"))
        telemetry.trackInstallState(BaseModelInstallState.Failed("Error"))

        assertTrue(telemetry.loggedEvents.isEmpty())
    }
}
