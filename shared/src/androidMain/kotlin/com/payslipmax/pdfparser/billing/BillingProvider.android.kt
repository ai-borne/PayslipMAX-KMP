package com.payslipmax.pdfparser.billing

import com.payslipmax.pdfparser.crypto.ContextHolder
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration

private val billingLock = Any()

@Volatile
private var cachedBillingManager: BillingManager? = null

actual fun provideBillingManager(): BillingManager {
    ContextHolder.context ?: return FakeBillingManager()
    val isTestEnvironment = android.os.Build.FINGERPRINT == "robolectric" || android.os.Build.HARDWARE == "robolectric"
    if (isTestEnvironment) return FakeBillingManager()

    cachedBillingManager?.let { return it }
    return synchronized(billingLock) {
        cachedBillingManager ?: run {
            if (!Purchases.isConfigured) {
                Purchases.configure(PurchasesConfiguration(revenueCatApiKey()))
            }
            RevenueCatBillingManager().also { cachedBillingManager = it }
        }
    }
}
