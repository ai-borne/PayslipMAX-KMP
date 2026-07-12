package com.payslipmax.pdfparser.telemetry

import android.annotation.SuppressLint
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.payslipmax.pdfparser.crypto.ContextHolder

@SuppressLint("MissingPermission")
class AndroidGemmaInstallTelemetry : BaseGemmaInstallTelemetry() {
    private val analytics: FirebaseAnalytics?
        get() {
            val context = ContextHolder.context ?: return null
            return try {
                FirebaseAnalytics.getInstance(context)
            } catch (e: Exception) {
                null
            }
        }

    override fun onTelemetryEnabledChanged(enabled: Boolean) {
        analytics?.setAnalyticsCollectionEnabled(enabled)
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        } catch (e: Exception) {
            // Ignore if Crashlytics not initialized
        }
    }

    override fun logEvent(
        name: String,
        params: Map<String, String>?,
    ) {
        if (!isEnabled) return
        val targetAnalytics = analytics ?: return
        val bundle =
            Bundle().apply {
                params?.forEach { (key, value) ->
                    putString(key, value)
                }
            }
        targetAnalytics.logEvent(name, bundle)
    }
}

actual fun provideGemmaInstallTelemetry(): GemmaInstallTelemetry = AndroidGemmaInstallTelemetry()
