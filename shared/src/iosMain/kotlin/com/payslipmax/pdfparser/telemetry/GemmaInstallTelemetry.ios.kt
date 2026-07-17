package com.payslipmax.pdfparser.telemetry

class IosGemmaInstallTelemetry : BaseGemmaInstallTelemetry() {
    override fun onTelemetryEnabledChanged(enabled: Boolean) {
        delegate?.onTelemetryEnabledChanged(enabled)
    }

    override fun logEvent(
        name: String,
        params: Map<String, String>?,
    ) {
        if (!isEnabled) return
        delegate?.logEvent(name, params)
    }

    companion object {
        var delegate: IosTelemetryDelegate? = null
    }
}

interface IosTelemetryDelegate {
    fun onTelemetryEnabledChanged(enabled: Boolean)

    fun logEvent(
        name: String,
        params: Map<String, String>?,
    )
}

actual fun provideGemmaInstallTelemetry(): GemmaInstallTelemetry = IosGemmaInstallTelemetry()
