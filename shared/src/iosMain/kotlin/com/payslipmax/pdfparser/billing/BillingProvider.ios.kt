package com.payslipmax.pdfparser.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import platform.Foundation.NSBundle

private var cachedBillingManager: BillingManager? = null

actual fun provideBillingManager(): BillingManager {
    val bundleId = NSBundle.mainBundle.bundleIdentifier
    if (bundleId == null || !bundleId.startsWith("com.payslipmax")) {
        return FakeBillingManager()
    }
    cachedBillingManager?.let { return it }
    if (!Purchases.isConfigured) {
        Purchases.configure(PurchasesConfiguration(revenueCatApiKey()))
    }
    return RevenueCatBillingManager().also { cachedBillingManager = it }
}
