package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class AndroidGemmaBaseModelInstallerTest {
    @Test
    fun initialStateIsNotStarted() {
        val installer = AndroidGemmaBaseModelInstaller()
        assertEquals(BaseModelInstallState.NotStarted, installer.state.value)
    }

    @Test
    fun installIsANoOpUntilPhase3WiresAssetPackManager() =
        runTest {
            val installer = AndroidGemmaBaseModelInstaller()
            installer.install()
            // Phase 3 wires AssetPackManager.requestFetch(); until then, install() must not claim
            // any state transition it can't actually back with a real download.
            assertEquals(BaseModelInstallState.NotStarted, installer.state.value)
        }
}
