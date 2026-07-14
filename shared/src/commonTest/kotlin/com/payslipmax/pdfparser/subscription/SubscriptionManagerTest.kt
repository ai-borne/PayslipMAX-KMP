package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionManagerTest {
    private fun debugManager(premium: () -> Boolean) =
        SubscriptionManager(isPremiumEnabledProvider = premium, isDebugBuildProvider = { true })

    private fun releaseManager(premium: () -> Boolean) =
        SubscriptionManager(isPremiumEnabledProvider = premium, isDebugBuildProvider = { false })

    @Test
    fun testDefaultOverrideIsForceProInDebug() {
        val manager = debugManager { false }
        assertEquals(DevOverride.FORCE_PRO, manager.devOverride.value)
        assertTrue(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
    }

    @Test
    fun testDefaultOverrideIsFollowFlagInRelease() {
        val manager = releaseManager { false }
        assertEquals(DevOverride.FOLLOW_FLAG, manager.devOverride.value)
    }

    @Test
    fun testForceFreeBlocksEvenWhenFlagTrue() {
        val manager = debugManager { true }
        manager.setDevOverride(DevOverride.FORCE_FREE)
        for (gate in FeatureGate.values()) {
            assertFalse(manager.hasAccess(gate), "FORCE_FREE must block $gate even when flag=true")
        }
    }

    @Test
    fun testForceProGrantsWhenFlagFalse() {
        val manager = debugManager { false }
        manager.setDevOverride(DevOverride.FORCE_PRO)
        for (gate in FeatureGate.values()) {
            assertTrue(manager.hasAccess(gate), "FORCE_PRO must grant $gate even when flag=false")
        }
    }

    @Test
    fun testFollowFlagTracksFlag() {
        var premium = false
        val manager = debugManager { premium }
        manager.setDevOverride(DevOverride.FOLLOW_FLAG)

        assertFalse(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        premium = true
        assertTrue(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
    }

    @Test
    fun testOverrideSetterInertInRelease() {
        val manager = releaseManager { false }
        manager.setDevOverride(DevOverride.FORCE_PRO)

        // Setter is a no-op in release: override stays at default and gating follows the flag only.
        assertEquals(DevOverride.FOLLOW_FLAG, manager.devOverride.value)
        assertFalse(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
    }

    @Test
    fun testReleaseIgnoresAnyOverrideAndFollowsFlag() {
        var premium = false
        val manager = releaseManager { premium }
        assertFalse(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        premium = true
        assertTrue(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
    }

    @Test
    fun testAllGatesAccessibleWhenFlagTrue() {
        val manager = releaseManager { true }
        for (gate in FeatureGate.values()) {
            assertTrue(manager.hasAccess(gate))
        }
    }
}
