package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertFalse
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
 * The Guide paywall is a separate switch from the dark launch: the owner turns it on only after clearing the open
 * points on the main rate cards, so it cannot ride along with the E9 release flip by accident.
 */
class GuidePaywallFlagTest {
    @Test
    fun guidePaywallStaysOffUntilTheOwnerClearsTheOpenRatePoints() {
        assertFalse(LaunchFlags.GUIDE_PAYWALL_ENABLED)
    }
}
