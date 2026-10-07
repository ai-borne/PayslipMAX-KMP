package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.subscription.DevOverride
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The paid half of the Guide opens for Premium, and for everyone while the paywall is off (owner decision
 * 2026-10-07). A QA `FORCE_FREE` still sees the locked screens with the paywall off; that override is inert in
 * production, so a real user can never be locked out by it.
 */
class GuideAccessTest {
    @Test
    fun withThePaywallOnOnlyPremiumIsUnlocked() {
        for (override in DevOverride.values()) {
            assertTrue(guideUnlocked(hasAccess = true, devOverride = override, paywallEnabled = true), "premium, $override")
            assertFalse(guideUnlocked(hasAccess = false, devOverride = override, paywallEnabled = true), "free, $override")
        }
    }

    @Test
    fun withThePaywallOffEveryoneIsUnlockedExceptQaForcingFree() {
        assertTrue(guideUnlocked(hasAccess = false, devOverride = DevOverride.FOLLOW_FLAG, paywallEnabled = false))
        assertTrue(guideUnlocked(hasAccess = false, devOverride = DevOverride.FORCE_PRO, paywallEnabled = false))
        assertFalse(guideUnlocked(hasAccess = false, devOverride = DevOverride.FORCE_FREE, paywallEnabled = false))
    }

    @Test
    fun theShippedDefaultIsThePaywallFlagSoNothingIsLockedBeforeTheOwnerDecides() {
        assertTrue(guideUnlocked(hasAccess = false, devOverride = DevOverride.FOLLOW_FLAG))
    }
}
