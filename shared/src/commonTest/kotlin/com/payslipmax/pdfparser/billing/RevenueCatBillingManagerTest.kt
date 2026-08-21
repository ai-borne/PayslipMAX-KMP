package com.payslipmax.pdfparser.billing

import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.EntitlementInfo
import com.revenuecat.purchases.kmp.models.EntitlementInfos
import com.revenuecat.purchases.kmp.models.OwnershipType
import com.revenuecat.purchases.kmp.models.PeriodType
import com.revenuecat.purchases.kmp.models.Store
import com.revenuecat.purchases.kmp.models.VerificationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [mapCustomerInfoToSubscriptionState] — the one piece of real business logic in
 * [RevenueCatBillingManager], unit-testable without hitting the RevenueCat SDK/network.
 */
class RevenueCatBillingManagerTest {
    private fun entitlementInfo(
        identifier: String,
        isActive: Boolean,
        willRenew: Boolean,
        expirationDateMillis: Long?,
    ) = EntitlementInfo(
        identifier,
        isActive,
        willRenew,
        PeriodType.NORMAL,
        null,
        null,
        expirationDateMillis,
        Store.PLAY_STORE,
        "com.payslipmax.pro.yearly",
        null,
        false,
        null,
        null,
        OwnershipType.PURCHASED,
        VerificationResult.NOT_REQUESTED,
    )

    private fun customerInfo(active: Map<String, EntitlementInfo>) =
        CustomerInfo(
            emptySet(),
            emptyMap(),
            emptyMap(),
            emptySet(),
            EntitlementInfos(active, VerificationResult.NOT_REQUESTED),
            0L,
            null,
            null,
            emptyMap(),
            emptyList(),
            "app_user_id",
            null,
            null,
            0L,
        )

    @Test
    fun mapsToActive_whenEntitlementIsActive() {
        val info =
            customerInfo(
                mapOf(
                    REVENUECAT_ENTITLEMENT_ID to
                        entitlementInfo(REVENUECAT_ENTITLEMENT_ID, isActive = true, willRenew = true, expirationDateMillis = 123L),
                ),
            )

        val state = mapCustomerInfoToSubscriptionState(info)

        assertTrue(state is SubscriptionState.Active)
        assertEquals(123L, state.expirationTimestampMs)
        assertTrue(state.autoRenewing)
    }

    @Test
    fun mapsToInactive_whenEntitlementExpired() {
        // EntitlementInfos.active only contains currently-active entitlements, so an expired
        // entitlement is absent from the map entirely — mirroring what the real SDK returns.
        val info = customerInfo(emptyMap())

        assertEquals(SubscriptionState.Inactive, mapCustomerInfoToSubscriptionState(info))
    }

    @Test
    fun mapsToInactive_whenNoEntitlementPresent() {
        val info = customerInfo(emptyMap())

        assertEquals(SubscriptionState.Inactive, mapCustomerInfoToSubscriptionState(info))
    }

    @Test
    fun mapsToActive_duringAppleGracePeriodOrBillingRetry() {
        val info =
            customerInfo(
                mapOf(
                    REVENUECAT_ENTITLEMENT_ID to
                        entitlementInfo(
                            REVENUECAT_ENTITLEMENT_ID,
                            isActive = true,
                            willRenew = true,
                            expirationDateMillis = 999999L,
                        ),
                ),
            )

        val state = mapCustomerInfoToSubscriptionState(info)
        assertTrue(state is SubscriptionState.Active)
        assertEquals(999999L, state.expirationTimestampMs)
        assertTrue(state.autoRenewing)
    }

    @Test
    fun mapsToActive_whenUserCancelledAutoRenewalButPeriodStillActive() {
        val info =
            customerInfo(
                mapOf(
                    REVENUECAT_ENTITLEMENT_ID to
                        entitlementInfo(
                            REVENUECAT_ENTITLEMENT_ID,
                            isActive = true,
                            willRenew = false,
                            expirationDateMillis = 888888L,
                        ),
                ),
            )

        val state = mapCustomerInfoToSubscriptionState(info)
        assertTrue(state is SubscriptionState.Active)
        assertEquals(888888L, state.expirationTimestampMs)
        kotlin.test.assertFalse(state.autoRenewing)
    }

    @Test
    fun ignoresOtherEntitlements_whenEntitlementIdDoesNotMatch() {
        val info =
            customerInfo(
                mapOf(
                    "some_other_entitlement" to
                        entitlementInfo("some_other_entitlement", isActive = true, willRenew = true, expirationDateMillis = 999L),
                ),
            )

        assertEquals(SubscriptionState.Inactive, mapCustomerInfoToSubscriptionState(info))
    }
}
