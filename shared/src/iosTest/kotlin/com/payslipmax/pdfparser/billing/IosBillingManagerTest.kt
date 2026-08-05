package com.payslipmax.pdfparser.billing

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosBillingManagerTest {
    @Test
    fun launchBillingFlow_returnsError_whenDelegateNotRegistered() =
        runTest {
            IosBillingManager.purchaseDelegate = null
            val manager = IosBillingManager()

            val result = manager.launchBillingFlow()
            assertTrue(result is PurchaseResult.Error)
            assertEquals("StoreKit bridge not registered", (result as PurchaseResult.Error).message)
        }

    @Test
    fun launchBillingFlow_returnsSuccess_whenDelegateSucceeds() =
        runTest {
            IosBillingManager.purchaseDelegate = { completion ->
                completion(true, "ios_trans_123", null)
            }
            val manager = IosBillingManager()

            val result = manager.launchBillingFlow()
            assertTrue(result is PurchaseResult.Success)
            assertEquals("ios_trans_123", (result as PurchaseResult.Success).purchaseToken)
            assertTrue(manager.subscriptionState.value is SubscriptionState.Active)
        }

    @Test
    fun launchBillingFlow_returnsUserCancelled_whenDelegateReportsCancel() =
        runTest {
            IosBillingManager.purchaseDelegate = { completion ->
                completion(false, null, "USER_CANCELLED")
            }
            val manager = IosBillingManager()

            val result = manager.launchBillingFlow()
            assertTrue(result is PurchaseResult.UserCancelled)
        }

    @Test
    fun restorePurchases_updatesState_whenDelegateSucceeds() =
        runTest {
            IosBillingManager.restoreDelegate = { completion ->
                completion(true, "restored_ios_123", null)
            }
            val manager = IosBillingManager()

            val result = manager.restorePurchases()
            assertTrue(result is PurchaseResult.Success)
            assertTrue(manager.subscriptionState.value is SubscriptionState.Active)
        }
}
