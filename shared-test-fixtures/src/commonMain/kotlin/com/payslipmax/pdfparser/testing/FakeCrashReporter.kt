package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.telemetry.CrashReporter

/** Records what would leave the device, so tests can assert telemetry carries codes only. */
class FakeCrashReporter : CrashReporter {
    val logs = mutableListOf<String>()
    val keys = mutableMapOf<String, String>()
    val exceptions = mutableListOf<Pair<Throwable, Map<String, String>?>>()

    override fun log(message: String) {
        logs += message
    }

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        keys[key] = value
    }

    override fun recordException(
        throwable: Throwable,
        metadata: Map<String, String>?,
    ) {
        exceptions += throwable to metadata
    }

    override fun setUserId(userId: String) = Unit
}
