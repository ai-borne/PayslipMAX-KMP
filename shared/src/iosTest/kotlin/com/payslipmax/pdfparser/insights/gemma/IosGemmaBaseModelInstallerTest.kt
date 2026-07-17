package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosGemmaBaseModelInstallerTest {
    @AfterTest
    fun cleanup() {
        // A delegate registered by one test must never leak into the next — same discipline as
        // GemmaEngine.inferenceDelegate's @AfterTest reset (IosGemmaEngineTest.kt).
        IosGemmaBaseModelInstaller.progressReporter = null
        IosGemmaBaseModelInstaller.completionReporter = null
    }

    @Test
    fun initialStateIsNotStartedWhenNoModelIsAlreadyInstalled() {
        // No real App Group entitlement is configured in a plain unit-test host, so
        // resolveInstalledGemmaModelPath() returns null here — this proves the installer
        // degrades to NotStarted rather than crashing when the container isn't available.
        val installer = IosGemmaBaseModelInstaller()
        assertEquals(BaseModelInstallState.NotStarted, installer.state.value)
    }

    @Test
    fun constructionRegistersBothReporters() {
        IosGemmaBaseModelInstaller()
        assertTrue(IosGemmaBaseModelInstaller.progressReporter != null)
        assertTrue(IosGemmaBaseModelInstaller.completionReporter != null)
    }

    @Test
    fun installIsANoOpTriggerButChecksForAnAlreadyInstalledModel() =
        runTest {
            // Background Assets schedules itself — install() can't trigger a real download here —
            // but a model that finished installing on a previous launch must be reflected immediately.
            val installer = IosGemmaBaseModelInstaller()
            installer.install()
            assertEquals(BaseModelInstallState.NotStarted, installer.state.value)
        }

    @Test
    fun progressReporterUpdatesStateToDownloading() {
        val installer = IosGemmaBaseModelInstaller()

        IosGemmaBaseModelInstaller.progressReporter?.invoke(50L, 200L)

        assertEquals(BaseModelInstallState.Downloading(0.25f), installer.state.value)
    }

    @Test
    fun progressReporterGuardsAgainstZeroTotalBytes() {
        val installer = IosGemmaBaseModelInstaller()

        IosGemmaBaseModelInstaller.progressReporter?.invoke(0L, 0L)

        assertEquals(BaseModelInstallState.Downloading(0f), installer.state.value)
    }

    @Test
    fun completionReporterFailureUpdatesStateToFailed() {
        val installer = IosGemmaBaseModelInstaller()

        IosGemmaBaseModelInstaller.completionReporter?.invoke(false, "disk full")

        assertEquals(BaseModelInstallState.Failed("disk full"), installer.state.value)
    }

    @Test
    fun completionReporterFailureWithoutMessageUsesADefaultMessage() {
        val installer = IosGemmaBaseModelInstaller()

        IosGemmaBaseModelInstaller.completionReporter?.invoke(false, null)

        val state = installer.state.value
        assertTrue(state is BaseModelInstallState.Failed)
        assertTrue(state.message.isNotEmpty())
    }

    @Test
    fun completionReporterSuccessWithoutAResolvablePathStillReportsInstalled() {
        // No real App Group entitlement in this test host, so resolveInstalledGemmaModelPath()
        // returns null even on a reported success — the installer must not crash, and must still
        // surface Installed (with an empty path) rather than silently staying stuck.
        val installer = IosGemmaBaseModelInstaller()

        IosGemmaBaseModelInstaller.completionReporter?.invoke(true, null)

        assertEquals(BaseModelInstallState.Installed(""), installer.state.value)
    }
}
