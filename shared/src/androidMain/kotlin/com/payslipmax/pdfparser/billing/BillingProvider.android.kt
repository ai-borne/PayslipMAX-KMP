package com.payslipmax.pdfparser.billing

import com.payslipmax.pdfparser.crypto.ContextHolder
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration

actual fun provideBillingManager(): BillingManager {
    ContextHolder.context ?: return FakeBillingManager()
    val isTestEnvironment = android.os.Build.FINGERPRINT == "robolectric" || android.os.Build.HARDWARE == "robolectric"
    if (isTestEnvironment) return FakeBillingManager()

    if (!Purchases.isConfigured) {
        Purchases.configure(PurchasesConfiguration(revenueCatApiKey()))
    }
    return RevenueCatBillingManager()
}
