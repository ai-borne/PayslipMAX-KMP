package com.payslipmax.pdfparser.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** RevenueCat entitlement identifier (Phase 0 dashboard setup). */
const val REVENUECAT_ENTITLEMENT_ID = "premium"

/** RevenueCat package identifier inside the default Offering (Phase 0 dashboard setup). */
private const val REVENUECAT_PACKAGE_ID = "yearly"

/**
 * Pure mapping from RevenueCat's [CustomerInfo] to this app's [SubscriptionState], unit-testable
 * without hitting the SDK/network.
 */
fun mapCustomerInfoToSubscriptionState(
    customerInfo: CustomerInfo,
    entitlementId: String = REVENUECAT_ENTITLEMENT_ID,
): SubscriptionState {
    val entitlement = customerInfo.entitlements.active[entitlementId] ?: return SubscriptionState.Inactive
    return SubscriptionState.Active(
        expirationTimestampMs = entitlement.expirationDateMillis ?: 0L,
        autoRenewing = entitlement.willRenew,
    )
}

/**
 * RevenueCat KMP implementation of [BillingManager]. Assumes [Purchases] has already been
 * configured with the app's API key (see the platform `provideBillingManager()` actuals).
 */
class RevenueCatBillingManager : BillingManager, PurchasesDelegate {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Unknown)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    init {
        Purchases.sharedInstance.delegate = this
        Purchases.sharedInstance.getCustomerInfo(
            onError = { _subscriptionState.value = SubscriptionState.Unknown },
            onSuccess = { info -> _subscriptionState.value = mapCustomerInfoToSubscriptionState(info) },
        )
    }

    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        _subscriptionState.value = mapCustomerInfoToSubscriptionState(customerInfo)
    }

    override fun onPurchasePromoProduct(
        product: StoreProduct,
        startPurchase: (
            onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
            onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
        ) -> Unit,
    ) {
        startPurchase(
            { _, _ -> },
            { _, info -> _subscriptionState.value = mapCustomerInfoToSubscriptionState(info) },
        )
    }

    override suspend fun launchBillingFlow(): PurchaseResult {
        val packageToPurchase =
            resolveYearlyPackage() ?: return PurchaseResult.Error("Package $REVENUECAT_PACKAGE_ID unavailable")

        return suspendCoroutine { continuation ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = packageToPurchase,
                onError = { error, userCancelled ->
                    continuation.resume(
                        if (userCancelled) {
                            PurchaseResult.UserCancelled
                        } else {
                            PurchaseResult.Error(error.message ?: "Purchase failed")
                        },
                    )
                },
                onSuccess = { transaction, customerInfo ->
                    _subscriptionState.value = mapCustomerInfoToSubscriptionState(customerInfo)
                    continuation.resume(PurchaseResult.Success(purchaseToken = transaction.transactionId ?: ""))
                },
            )
        }
    }

    override suspend fun restorePurchases(): PurchaseResult =
        suspendCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchases(
                onError = { error -> continuation.resume(PurchaseResult.Error(error.message ?: "Restore failed")) },
                onSuccess = { customerInfo ->
                    _subscriptionState.value = mapCustomerInfoToSubscriptionState(customerInfo)
                    val restored = customerInfo.entitlements.active.containsKey(REVENUECAT_ENTITLEMENT_ID)
                    continuation.resume(
                        if (restored) {
                            PurchaseResult.Success(purchaseToken = customerInfo.originalAppUserId ?: "")
                        } else {
                            PurchaseResult.Error("No active verified subscription found")
                        },
                    )
                },
            )
        }

    override suspend fun getFormattedPrice(): String? = resolveYearlyPackage()?.storeProduct?.price?.formatted

    private suspend fun resolveYearlyPackage(): Package? =
        suspendCoroutine { continuation ->
            Purchases.sharedInstance.getOfferings(
                onError = { continuation.resume(null) },
                onSuccess = { offerings -> continuation.resume(offerings.current?.getPackage(REVENUECAT_PACKAGE_ID)) },
            )
        }
}
