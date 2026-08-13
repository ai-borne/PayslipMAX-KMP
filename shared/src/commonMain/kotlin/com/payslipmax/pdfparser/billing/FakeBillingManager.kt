package com.payslipmax.pdfparser.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory mock implementation of [BillingManager] for unit testing and debug sandbox simulation.
 */
class FakeBillingManager(
    initialState: SubscriptionState = SubscriptionState.Inactive,
) : BillingManager {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(initialState)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    var shouldPurchaseSucceed: Boolean = true
    var shouldRestoreSucceed: Boolean = true
    var mockErrorMessage: String = "Purchase failed"
    var fakeFormattedPrice: String? = null

    override suspend fun launchBillingFlow(): PurchaseResult {
        return if (shouldPurchaseSucceed) {
            _subscriptionState.value = SubscriptionState.Active()
            PurchaseResult.Success(purchaseToken = "fake_token_12345")
        } else {
            PurchaseResult.Error(mockErrorMessage)
        }
    }

    override suspend fun restorePurchases(): PurchaseResult {
        return if (shouldRestoreSucceed) {
            _subscriptionState.value = SubscriptionState.Active()
            PurchaseResult.Success(purchaseToken = "restored_token_12345")
        } else {
            PurchaseResult.Error(mockErrorMessage)
        }
    }

    override suspend fun getFormattedPrice(): String? = fakeFormattedPrice

    fun setSubscriptionState(state: SubscriptionState) {
        _subscriptionState.value = state
    }
}
