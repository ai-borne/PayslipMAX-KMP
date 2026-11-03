package com.payslipmax.pdfparser.logging

import com.payslipmax.pdfparser.subscription.isDebugBuild

object Logger {
    private val platformReporter: com.payslipmax.pdfparser.telemetry.CrashReporter by lazy {
        com.payslipmax.pdfparser.telemetry.provideCrashReporter()
    }

    /**
     * The app's reporter from Koin once it is started (the shared module binds the same platform reporter; a test binds a fake, so it can
     * read what each log site sends), else the platform's own. No state is kept here.
     */
    private val crashReporter: com.payslipmax.pdfparser.telemetry.CrashReporter
        get() = org.koin.mp.KoinPlatform.getKoinOrNull()?.getOrNull<com.payslipmax.pdfparser.telemetry.CrashReporter>() ?: platformReporter

    fun d(
        tag: String,
        message: String,
    ) {
        if (isDebugBuild()) {
            println("[DEBUG] [$tag] $message")
        }
    }

    fun w(
        tag: String,
        message: String,
    ) {
        println("[WARN] [$tag] $message")
        crashReporter.log("WARN [$tag] $message")
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val errorMsg = throwable?.let { " - ${it.message}" } ?: ""
        println("[ERROR] [$tag] $message$errorMsg")
        crashReporter.log("ERROR [$tag] $message")
        if (throwable != null) {
            crashReporter.recordException(throwable, mapOf("error_tag" to tag))
        }
    }
}
