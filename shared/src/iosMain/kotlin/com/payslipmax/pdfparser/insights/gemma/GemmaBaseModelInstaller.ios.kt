package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 1 placeholder: Background Assets schedules its download autonomously (device charging,
 * on Wi-Fi, not in Low Power Mode), so [install] can never be a trigger — Phase 4 wires
 * `GemmaBackgroundAssetsBridge.swift` to forward the extension's progress/completion into this
 * state instead. Until then this reports [BaseModelInstallState.NotStarted] and never transitions.
 */
class IosGemmaBaseModelInstaller : GemmaBaseModelInstaller {
    private val _state = MutableStateFlow<BaseModelInstallState>(BaseModelInstallState.NotStarted)
    override val state: StateFlow<BaseModelInstallState> = _state.asStateFlow()

    override suspend fun install() {
        // No-op: Background Assets schedules itself. Phase 4 wires the Swift bridge's reporters.
    }
}

actual fun provideGemmaBaseModelInstaller(): GemmaBaseModelInstaller = IosGemmaBaseModelInstaller()
