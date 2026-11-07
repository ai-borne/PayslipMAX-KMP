package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the Orders Bank is Premium with no free allowance, and the gate must be real money logic, not hidden behind the free-launch switch:
 * a release build with the launch mode off and no entitlement is locked, an entitled officer is open, and the debug overrides reach both
 * states so the locked screens can be walked. The gate says who may ADD; viewing, deleting and backing up a lapsed officer's Bank is the
 * screens' rule (tested there), because storage never asks the gate.
 */
class Part2OrdersGateTest {
    private fun release(
        premium: Boolean,
        freeLaunch: Boolean = false,
    ) = SubscriptionManager(
        isPremiumEnabledProvider = { premium },
        isDebugBuildProvider = { false },
        isFreeLaunchModeProvider = { freeLaunch },
        isTestFlightBuildProvider = { false },
    )

    @Test
    fun aReleaseBuildWithoutEntitlementIsLockedAndWithItIsOpen() {
        assertFalse(release(premium = false).hasAccess(FeatureGate.PART2_ORDERS))
        assertTrue(release(premium = true).hasAccess(FeatureGate.PART2_ORDERS))
    }

    @Test
    fun freeLaunchModeOpensItLikeEveryGate() {
        assertTrue(release(premium = false, freeLaunch = true).hasAccess(FeatureGate.PART2_ORDERS))
    }

    @Test
    fun theDebugOverridesReachBothStates() {
        val debug =
            SubscriptionManager(
                isPremiumEnabledProvider = { false },
                isDebugBuildProvider = { true },
                isFreeLaunchModeProvider = { false },
                isTestFlightBuildProvider = { false },
            )
        assertTrue(debug.hasAccess(FeatureGate.PART2_ORDERS), "FORCE_PRO is the debug default")
        debug.setDevOverride(DevOverride.FORCE_FREE)
        assertFalse(debug.hasAccess(FeatureGate.PART2_ORDERS), "FORCE_FREE reaches the locked Bank")
        debug.setDevOverride(DevOverride.FOLLOW_FLAG)
        assertFalse(debug.hasAccess(FeatureGate.PART2_ORDERS), "following the flag, an unentitled officer is locked")
    }
}
