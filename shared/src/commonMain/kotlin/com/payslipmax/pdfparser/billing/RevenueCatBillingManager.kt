package com.payslipmax.pdfparser.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * RevenueCat entitlement identifier. Must match the dashboard's literal "Identifier" field
 * (Product catalog → Entitlements) exactly, not a slugified guess — this project's entitlement
 * identifier is "PayslipMax Premium" (with the space), confirmed against the live dashboard in
 * Phase 5; the code previously assumed "premium" which would never have matched a real purchase.
 */
const val REVENUECAT_ENTITLEMENT_ID = "PayslipMax Premium"

/**
 * Pure resolution of the purchasable yearly package from a fetched [Offerings], unit-testable
 * without the SDK.
 *
 * Resolves via [Offering.annual] (the SDK's typed accessor for the predefined `$rc_annual` package
 * type) rather than a literal package identifier. The dashboard's package identifier is
 * `$rc_annual`; `"yearly"` is the identifier of the *Test Store product inside* that package, not
 * the package itself. Looking up `getPackage("yearly")` therefore never matched, so every purchase
 * failed with "package unavailable" and the paywall silently fell back to a hardcoded price.
 *
 * Returns a [YearlyPackageResolution] rather than a nullable package so the two dashboard
 * misconfigurations — no Current offering, versus a Current offering with an empty annual slot —
 * stay distinguishable instead of collapsing into one opaque failure.
 */
fun resolveYearlyPackageFrom(offerings: Offerings): YearlyPackageResolution {
    val currentOffering = offerings.current ?: return YearlyPackageResolution.NoCurrentOffering
    val annual = currentOffering.annual ?: return YearlyPackageResolution.NoAnnualPackage
    return YearlyPackageResolution.Resolved(annual)
}

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
            when (val resolution = resolveYearlyPackage()) {
                is YearlyPackageResolution.Resolved -> resolution.yearlyPackage
                is YearlyPackageResolution.Failure -> return PurchaseResult.Error(resolution.message)
            }

        return suspendCoroutine { continuation ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = packageToPurchase,
                onError = { error, userCancelled ->
                    continuation.resume(
                        if (userCancelled) {
                            PurchaseResult.UserCancelled
                        } else {
                            PurchaseResult.Error(error.message)
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
                onError = { error -> continuation.resume(PurchaseResult.Error(error.message)) },
                onSuccess = { customerInfo ->
                    _subscriptionState.value = mapCustomerInfoToSubscriptionState(customerInfo)
                    val restored = customerInfo.entitlements.active.containsKey(REVENUECAT_ENTITLEMENT_ID)
                    continuation.resume(
                        if (restored) {
                            PurchaseResult.Success(purchaseToken = customerInfo.originalAppUserId)
                        } else {
                            PurchaseResult.Error("No active verified subscription found")
                        },
                    )
                },
            )
        }

    override suspend fun getFormattedPrice(): String? =
        (resolveYearlyPackage() as? YearlyPackageResolution.Resolved)
            ?.yearlyPackage
            ?.storeProduct
            ?.price
            ?.formatted

    private suspend fun resolveYearlyPackage(): YearlyPackageResolution =
        suspendCoroutine { continuation ->
            Purchases.sharedInstance.getOfferings(
                onError = { error ->
                    continuation.resume(
                        YearlyPackageResolution.OfferingsUnavailable(
                            error.message,
                        ),
                    )
                },
                onSuccess = { offerings -> continuation.resume(resolveYearlyPackageFrom(offerings)) },
            )
        }
}
