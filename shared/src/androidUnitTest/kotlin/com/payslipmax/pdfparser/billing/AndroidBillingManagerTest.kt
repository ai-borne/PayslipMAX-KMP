package com.payslipmax.pdfparser.billing

import androidx.test.core.app.ApplicationProvider
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidBillingManagerTest {
    @Test
    fun onPurchasesUpdated_updatesState_whenSuccess() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val manager = AndroidBillingManager(context)

            val mockPurchase = mockk<Purchase>(relaxed = true)
            every { mockPurchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { mockPurchase.purchaseToken } returns "token_android_123"

            val mockBillingResult = mockk<BillingResult>()
            every { mockBillingResult.responseCode } returns BillingClient.BillingResponseCode.OK

            manager.onPurchasesUpdated(mockBillingResult, listOf(mockPurchase))

            assertTrue(manager.subscriptionState.value is SubscriptionState.Active)
        }

    @Test
    fun onPurchasesUpdated_setsInactive_whenCanceled() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val manager = AndroidBillingManager(context)

            val mockBillingResult = mockk<BillingResult>()
            every { mockBillingResult.responseCode } returns BillingClient.BillingResponseCode.USER_CANCELED

            manager.onPurchasesUpdated(mockBillingResult, null)

            assertTrue(manager.subscriptionState.value is SubscriptionState.Inactive)
        }
}
