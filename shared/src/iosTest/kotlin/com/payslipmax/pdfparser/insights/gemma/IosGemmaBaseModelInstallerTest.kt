package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IosGemmaBaseModelInstallerTest {
    @Test
    fun initialStateIsNotStarted() {
        val installer = IosGemmaBaseModelInstaller()
        assertEquals(BaseModelInstallState.NotStarted, installer.state.value)
    }

    @Test
    fun installIsANoOpUntilPhase4WiresTheSwiftBridge() =
        runTest {
            val installer = IosGemmaBaseModelInstaller()
            installer.install()
            // Background Assets schedules itself; install() can't trigger anything real until
            // Phase 4 registers the Swift bridge's progress/completion reporters.
            assertEquals(BaseModelInstallState.NotStarted, installer.state.value)
        }
}
