package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionManagerTest {
    private fun debugManager(premium: () -> Boolean) =
        SubscriptionManager(
            isPremiumEnabledProvider = premium,
            isDebugBuildProvider = { true },
            isFreeLaunchModeProvider = { false },
        )

    private fun releaseManager(premium: () -> Boolean) =
        SubscriptionManager(
            isPremiumEnabledProvider = premium,
            isDebugBuildProvider = { false },
            isFreeLaunchModeProvider = { false },
        )

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

    @Test
    fun testFreeLaunchModeGrantsEveryGateInReleaseRegardlessOfFlagOrBilling() {
        val manager =
            SubscriptionManager(
                isPremiumEnabledProvider = { false },
                isDebugBuildProvider = { false },
                billingManager = null,
                isFreeLaunchModeProvider = { true },
            )
        for (gate in FeatureGate.values()) {
            assertTrue(manager.hasAccess(gate), "free launch mode must grant $gate even with no billing/flag")
        }
    }

    @Test
    fun testDebugForceFreeStillBlocksEvenWhenFreeLaunchModeIsOn() {
        val manager =
            SubscriptionManager(
                isPremiumEnabledProvider = { false },
                isDebugBuildProvider = { true },
                isFreeLaunchModeProvider = { true },
            )
        manager.setDevOverride(DevOverride.FORCE_FREE)
        for (gate in FeatureGate.values()) {
            assertFalse(manager.hasAccess(gate), "FORCE_FREE must still block $gate so QA can test locked UX during free launch")
        }
    }

    @Test
    fun testTestFlightBuildCanForceFreeToRevealRealPaywallWithoutTouchingFreeLaunchFlag() {
        // Phase 7: production flag stays true (free), but a TestFlight/sandbox install is override-eligible.
        val manager =
            SubscriptionManager(
                isPremiumEnabledProvider = { false },
                isDebugBuildProvider = { false },
                isFreeLaunchModeProvider = { true },
                isTestFlightBuildProvider = { true },
            )
        manager.setDevOverride(DevOverride.FORCE_FREE)
        for (gate in FeatureGate.values()) {
            assertFalse(manager.hasAccess(gate), "TestFlight FORCE_FREE must reach the real paywall even though production free-launch mode is on")
        }
    }

    @Test
    fun testOverrideStaysInertInRealProductionEvenWithFreeLaunchModeOff() {
        // Not debug, not TestFlight: this is the real App Store production shape.
        val manager =
            SubscriptionManager(
                isPremiumEnabledProvider = { false },
                isDebugBuildProvider = { false },
                isFreeLaunchModeProvider = { false },
                isTestFlightBuildProvider = { false },
            )
        manager.setDevOverride(DevOverride.FORCE_PRO)
        assertEquals(DevOverride.FOLLOW_FLAG, manager.devOverride.value, "override setter must be a no-op in real production")
        assertFalse(manager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
    }

    @Test
    fun testIosAndAndroidFreeLaunchFlagsAreReadIndependently() {
        // A test double standing in for isFreeLaunchModePlatform(): each platform's manager only
        // reacts to its own flag, proving the flags aren't accidentally coupled through a shared read.
        var iosFlag = true
        var androidFlag = true
        val iosManager =
            SubscriptionManager(
                isPremiumEnabledProvider = { false },
                isDebugBuildProvider = { false },
                isFreeLaunchModeProvider = { iosFlag },
            )
        val androidManager =
            SubscriptionManager(
                isPremiumEnabledProvider = { false },
                isDebugBuildProvider = { false },
                isFreeLaunchModeProvider = { androidFlag },
            )

        assertTrue(iosManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        assertTrue(androidManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))

        iosFlag = false
        assertFalse(iosManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE), "iOS gate must follow only its own flag")
        assertTrue(androidManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE), "Android gate must stay unaffected by iOS's flag flipping")
    }

    @Test
    fun theClaimGuideGateFollowsTheSameRulesAsEveryOtherGate() {
        // Plan E5: one FeatureGate value, no special case in hasAccess; the paywall switch lives above it (guideUnlocked).
        var premium = false
        val release = releaseManager { premium }
        assertFalse(release.hasAccess(FeatureGate.CLAIM_GUIDE))
        premium = true
        assertTrue(release.hasAccess(FeatureGate.CLAIM_GUIDE))

        val debug = debugManager { true }
        assertTrue(debug.hasAccess(FeatureGate.CLAIM_GUIDE), "FORCE_PRO is the debug default")
        debug.setDevOverride(DevOverride.FORCE_FREE)
        assertFalse(debug.hasAccess(FeatureGate.CLAIM_GUIDE), "FORCE_FREE reaches the locked Guide")

        val freeLaunch =
            SubscriptionManager(isPremiumEnabledProvider = { false }, isDebugBuildProvider = { false }, isFreeLaunchModeProvider = { true })
        assertTrue(freeLaunch.hasAccess(FeatureGate.CLAIM_GUIDE), "free-launch mode opens it like every gate")
    }
}
