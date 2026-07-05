package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionManagerTest {
    @Test
    fun testAccessBlockedByDefault() {
        var premium = false
        val manager = SubscriptionManager(isPremiumEnabledProvider = { premium })

        assertFalse(manager.isDeveloperPro)
        assertFalse(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        assertFalse(manager.hasAccess(FeatureGate.WEALTH_OPTIMIZATION))
        assertFalse(manager.hasAccess(FeatureGate.AI_AUDIT))
    }

    @Test
    fun testAccessGrantedWhenPremium() {
        var premium = true
        val manager = SubscriptionManager(isPremiumEnabledProvider = { premium })

        assertTrue(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        assertTrue(manager.hasAccess(FeatureGate.WEALTH_OPTIMIZATION))
        assertTrue(manager.hasAccess(FeatureGate.AI_AUDIT))
    }

    @Test
    fun testDeveloperProOverride() {
        var premium = false
        val manager = SubscriptionManager(isPremiumEnabledProvider = { premium })
        manager.enableDeveloperPro()

        assertTrue(manager.isDeveloperPro)
        assertTrue(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        assertTrue(manager.hasAccess(FeatureGate.WEALTH_OPTIMIZATION))
        assertTrue(manager.hasAccess(FeatureGate.AI_AUDIT))
    }

    @Test
    fun testAllGatesAccessible() {
        val manager = SubscriptionManager(isPremiumEnabledProvider = { true })
        for (gate in FeatureGate.values()) {
            assertTrue(manager.hasAccess(gate))
        }
    }
}
