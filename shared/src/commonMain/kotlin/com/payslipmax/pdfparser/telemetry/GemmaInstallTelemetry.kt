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
                    mapOf("error" to GEMMA_INSTALL_FAILED_CODE),
                )
            }
            else -> {
                // NotStarted, NeedsUserConfirmation: do nothing
            }
        }
    }
}

expect fun provideGemmaInstallTelemetry(): GemmaInstallTelemetry

/** The one value a failed install sends as its `error` parameter: the installer's own text stays on the device (docs/Plan/14a P10 follow-up). */
const val GEMMA_INSTALL_FAILED_CODE = "install_failed"
