package com.payslipmax.pdfparser.telemetry

import com.payslipmax.pdfparser.insights.gemma.BaseModelInstallState

interface GemmaInstallTelemetry {
    fun setTelemetryEnabled(enabled: Boolean)

    fun logEvent(
        name: String,
        params: Map<String, String>? = null,
    )

    fun trackInstallState(state: BaseModelInstallState)
}

abstract class BaseGemmaInstallTelemetry : GemmaInstallTelemetry {
    protected var isEnabled = true
    private var lastReportedBoundary: Int? = null

    override fun setTelemetryEnabled(enabled: Boolean) {
        isEnabled = enabled
        onTelemetryEnabledChanged(enabled)
    }

    protected abstract fun onTelemetryEnabledChanged(enabled: Boolean)

    override fun trackInstallState(state: BaseModelInstallState) {
        if (!isEnabled) return
        when (state) {
            is BaseModelInstallState.Downloading -> {
                val percent = (state.progress * 100).toInt()
                val currentBoundary =
                    when {
                        percent >= 75 -> 75
                        percent >= 50 -> 50
                        percent >= 25 -> 25
                        percent >= 0 -> 0
                        else -> null
                    }
                if (currentBoundary != null && currentBoundary != lastReportedBoundary) {
                    lastReportedBoundary = currentBoundary
                    logEvent(
                        "gemma_install_progress",
                        mapOf("progress" to "$currentBoundary%"),
                    )
                }
            }
            is BaseModelInstallState.Installed -> {
                lastReportedBoundary = null
                logEvent("gemma_install_success")
            }
            is BaseModelInstallState.Failed -> {
                lastReportedBoundary = null
                logEvent(
                    "gemma_install_failed",
                    mapOf("error" to state.message),
                )
            }
            else -> {
                // NotStarted, NeedsUserConfirmation: do nothing
            }
        }
    }
}

expect fun provideGemmaInstallTelemetry(): GemmaInstallTelemetry
