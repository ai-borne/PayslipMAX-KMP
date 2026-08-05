package com.payslipmax.pdfparser.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Android Google Play Billing v7 implementation of [BillingManager].
 */
class AndroidBillingManager(
    private val context: Context,
    private val activityProvider: () -> Activity? = { null },
) : BillingManager, PurchasesUpdatedListener {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Inactive)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private var pendingContinuation: ((PurchaseResult) -> Unit)? = null

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
    }

    override suspend fun launchBillingFlow(): PurchaseResult {
        val connected = connectIfNeeded()
        if (!connected) {
            return PurchaseResult.Error("Google Play Store connection unavailable")
        }

        val productDetails =
            queryProductDetails()
                ?: return PurchaseResult.Error("Product com.payslipmax.pro.yearly unavailable")

        val activity =
            activityProvider()
                ?: return PurchaseResult.Error("Activity context required for Play Billing checkout")

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()

        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

        return suspendCoroutine { continuation ->
            pendingContinuation = { result -> continuation.resume(result) }
            val response = billingClient.launchBillingFlow(activity, billingFlowParams)
            if (response.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingContinuation = null
                continuation.resume(PurchaseResult.Error("Failed to launch billing: ${response.debugMessage}"))
            }
        }
    }

    override suspend fun restorePurchases(): PurchaseResult {
        val connected = connectIfNeeded()
        if (!connected) return PurchaseResult.Error("Billing client not connected")

        val params =
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

        return suspendCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases.isNotEmpty()) {
                    val activePurchase = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    if (activePurchase != null) {
                        _subscriptionState.value = SubscriptionState.Active()
                        continuation.resume(PurchaseResult.Success(activePurchase.purchaseToken))
                    } else {
                        continuation.resume(PurchaseResult.Error("No active verified subscription found"))
                    }
                } else {
                    continuation.resume(PurchaseResult.Error("Restore failed: ${billingResult.debugMessage}"))
                }
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase != null && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgeIfNeeded(purchase)
                    _subscriptionState.value = SubscriptionState.Active()
                    pendingContinuation?.invoke(PurchaseResult.Success(purchase.purchaseToken))
                } else {
                    pendingContinuation?.invoke(PurchaseResult.Pending)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                pendingContinuation?.invoke(PurchaseResult.UserCancelled)
            }
            else -> {
                pendingContinuation?.invoke(PurchaseResult.Error(billingResult.debugMessage.ifBlank { "Purchase failed" }))
            }
        }
        pendingContinuation = null
    }

    private suspend fun connectIfNeeded(): Boolean =
        suspendCoroutine { continuation ->
            if (billingClient.isReady) {
                continuation.resume(true)
                return@suspendCoroutine
            }
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        // Connection lost
                    }
                },
            )
        }

    private suspend fun queryProductDetails(): ProductDetails? =
        suspendCoroutine { continuation ->
            val productList =
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                )
            val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(productDetailsList.firstOrNull())
                } else {
                    continuation.resume(null)
                }
            }
        }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val params =
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            billingClient.acknowledgePurchase(params) { }
        }
    }

    companion object {
        const val PRODUCT_ID = "com.payslipmax.pro.yearly"
    }
}
