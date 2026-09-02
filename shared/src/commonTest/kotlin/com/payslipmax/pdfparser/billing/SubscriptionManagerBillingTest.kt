package com.payslipmax.pdfparser.billing

import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.subscription.SubscriptionManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionManagerBillingTest {
    @Test
    fun hasAccess_follows_billingManager_when_in_release_and_followFlag() =
        runTest {
            val fakeBillingManager = FakeBillingManager(SubscriptionState.Inactive)
            // Release build configuration
            val subscriptionManager =
                SubscriptionManager(
                    isPremiumEnabledProvider = { false },
                    isDebugBuildProvider = { false },
                    billingManager = fakeBillingManager,
                )

            assertFalse(subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))

            // Trigger purchase
            val result = fakeBillingManager.launchBillingFlow()
            assertTrue(result is PurchaseResult.Success)

            assertTrue(subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        }

    @Test
    fun hasAccess_respects_devOverride_in_debug_build() =
        runTest {
            val fakeBillingManager = FakeBillingManager(SubscriptionState.Inactive)
            // Debug build configuration
            val subscriptionManager =
                SubscriptionManager(
                    isPremiumEnabledProvider = { false },
                    isDebugBuildProvider = { true },
                    billingManager = fakeBillingManager,
                )

            // Default in debug is FORCE_PRO
            assertTrue(subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))

            // Set to FOLLOW_FLAG
            subscriptionManager.setDevOverride(DevOverride.FOLLOW_FLAG)
            assertFalse(subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))

            // Activate purchase via billing manager
            fakeBillingManager.launchBillingFlow()
            assertTrue(subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        }

    @Test
    fun restorePurchases_updates_subscription_state() =
        runTest {
            val fakeBillingManager = FakeBillingManager(SubscriptionState.Inactive)
            val subscriptionManager =
                SubscriptionManager(
                    isPremiumEnabledProvider = { false },
                    isDebugBuildProvider = { false },
                    billingManager = fakeBillingManager,
                )

            assertFalse(subscriptionManager.hasAccess(FeatureGate.BACKUP_RESTORE))
            fakeBillingManager.restorePurchases()
            assertTrue(subscriptionManager.hasAccess(FeatureGate.BACKUP_RESTORE))
        }
}
