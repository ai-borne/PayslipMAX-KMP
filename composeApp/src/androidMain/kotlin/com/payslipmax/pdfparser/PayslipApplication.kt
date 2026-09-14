package com.payslipmax.pdfparser

import android.app.Application
import com.payslipmax.pdfparser.di.appModule
import com.payslipmax.pdfparser.di.sharedModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class PayslipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.payslipmax.pdfparser.crypto.ContextHolder.context = this
        try {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    crashlytics.setCustomKey("fatal_thread", thread.name)
                    crashlytics.recordException(throwable)
                } catch (_: Throwable) {
                } finally {
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        } catch (_: Exception) {
            // No-op under unit test runners where Firebase is not initialized
        }
        // Idempotent: Koin's GlobalContext is a JVM-wide singleton. Real devices only ever call
        // onCreate() once per process, so this guard is a no-op there — it only matters under
        // Robolectric, where multiple test classes share a JVM fork and each reconstructs this
        // Application, which would otherwise throw KoinApplicationAlreadyStartedException whenever
        // a prior test's stopKoin() teardown hadn't run yet in that fork.
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(sharedModule, appModule)
            }
        }

        try {
            val installationIdManager = GlobalContext.get().get<com.payslipmax.pdfparser.telemetry.InstallationIdManager>()
            val anonId = installationIdManager.getOrCreateInstallationId()
            val crashReporter = GlobalContext.get().get<com.payslipmax.pdfparser.telemetry.CrashReporter>()
            crashReporter.setUserId(anonId)
        } catch (_: Exception) {
            // Safe guard during unit tests
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE
        ) {
            com.payslipmax.pdfparser.insights.gemma.GemmaEngine.clearCache()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        com.payslipmax.pdfparser.insights.gemma.GemmaEngine.clearCache()
    }
}
