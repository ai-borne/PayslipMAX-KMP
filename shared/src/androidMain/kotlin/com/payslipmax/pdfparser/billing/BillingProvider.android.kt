package com.payslipmax.pdfparser.billing

import com.payslipmax.pdfparser.crypto.ContextHolder

actual fun provideBillingManager(): BillingManager {
    val context = ContextHolder.context ?: return FakeBillingManager()
    val isTestEnvironment = android.os.Build.FINGERPRINT == "robolectric" || android.os.Build.HARDWARE == "robolectric"
    return if (isTestEnvironment) {
        FakeBillingManager()
    } else {
        AndroidBillingManager(context = context)
    }
}
