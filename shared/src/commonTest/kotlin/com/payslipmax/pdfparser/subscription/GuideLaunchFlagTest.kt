package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The Claim Guide is on in release since phase E9 (2026-10-09, security review and end-to-end tests done). This
 * fails if the flag is switched off again, so pulling the Guide is a deliberate edit to this test as well.
 */
class GuideLaunchFlagTest {
    @Test
    fun guideIsOnInReleaseFromPhaseE9() {
        assertTrue(LaunchFlags.GUIDE_ENABLED)
    }
}

/**
 * The Guide paywall is a separate switch from `LaunchFlags.GUIDE_ENABLED`. The owner turned it on 2026-10-18, waiving five
 * unverified rate cards; this fails if it is switched off again, so that is a deliberate edit to this test as well.
 */
class GuidePaywallFlagTest {
    @Test
    fun guidePaywallIsOnFromTheOwnersFlipOf20261018() {
        assertTrue(LaunchFlags.GUIDE_PAYWALL_ENABLED)
    }
}
