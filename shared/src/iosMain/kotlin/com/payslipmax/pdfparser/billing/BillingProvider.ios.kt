package com.payslipmax.pdfparser.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration

actual fun provideBillingManager(): BillingManager {
    if (!Purchases.isConfigured) {
        Purchases.configure(PurchasesConfiguration(revenueCatApiKey()))
    }
    return RevenueCatBillingManager()
}
