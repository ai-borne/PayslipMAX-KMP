package com.payslipmax.pdfparser.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic In-App Purchase and entitlement management interface.
 */
interface BillingManager {
    val subscriptionState: StateFlow<SubscriptionState>

    suspend fun launchBillingFlow(): PurchaseResult

    suspend fun restorePurchases(): PurchaseResult
}
