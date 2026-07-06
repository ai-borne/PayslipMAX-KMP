package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 1 placeholder: Play Asset Delivery wiring (`AssetPackManager.requestFetch()`, the
 * cellular-consent dialog, `getPackLocation`) lands in Phase 3. Until then this reports
 * [BaseModelInstallState.NotStarted] and never transitions, so the app builds and runs against the
 * new contract with Tier 6 architecturally wired but inert on real devices.
 */
class AndroidGemmaBaseModelInstaller : GemmaBaseModelInstaller {
    private val _state = MutableStateFlow<BaseModelInstallState>(BaseModelInstallState.NotStarted)
    override val state: StateFlow<BaseModelInstallState> = _state.asStateFlow()

    override suspend fun install() {
        // No-op until Phase 3 wires AssetPackManager.requestFetch().
    }
}

actual fun provideGemmaBaseModelInstaller(): GemmaBaseModelInstaller = AndroidGemmaBaseModelInstaller()
