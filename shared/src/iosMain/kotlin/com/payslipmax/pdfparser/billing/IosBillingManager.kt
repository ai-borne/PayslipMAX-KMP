package com.payslipmax.pdfparser.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS actual implementation of [BillingManager].
 * Interoperates with StoreKit 2 via [StoreKitBillingBridge.swift] delegate closures.
 */
class IosBillingManager : BillingManager {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Inactive)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    init {
        statusUpdateListener = { state ->
            _subscriptionState.value = state
        }
    }

    override suspend fun launchBillingFlow(): PurchaseResult {
        val delegate = purchaseDelegate ?: return PurchaseResult.Error("StoreKit bridge not registered")
        return suspendCoroutine { continuation ->
            delegate { success, token, error ->
                if (success) {
                    _subscriptionState.value = SubscriptionState.Active()
                    continuation.resume(PurchaseResult.Success(token ?: ""))
                } else if (error == "USER_CANCELLED") {
                    continuation.resume(PurchaseResult.UserCancelled)
                } else {
                    continuation.resume(PurchaseResult.Error(error ?: "StoreKit purchase failed"))
                }
            }
        }
    }

    override suspend fun restorePurchases(): PurchaseResult {
        val delegate = restoreDelegate ?: return PurchaseResult.Error("StoreKit bridge not registered")
        return suspendCoroutine { continuation ->
            delegate { success, token, error ->
                if (success) {
                    _subscriptionState.value = SubscriptionState.Active()
                    continuation.resume(PurchaseResult.Success(token ?: ""))
                } else {
                    continuation.resume(PurchaseResult.Error(error ?: "No active subscriptions restored"))
                }
            }
        }
    }

    companion object {
        /** Invoked by StoreKitBillingBridge.swift when transaction status changes. */
        var statusUpdateListener: ((SubscriptionState) -> Unit)? = null

        /** Delegate closure registered by Swift entry point for purchase execution: (completion: (success, token, error) -> Unit) -> Unit */
        var purchaseDelegate: ((((Boolean, String?, String?) -> Unit)) -> Unit)? = null

        /** Delegate closure registered by Swift entry point for restoring purchases. */
        var restoreDelegate: ((((Boolean, String?, String?) -> Unit)) -> Unit)? = null
    }
}
