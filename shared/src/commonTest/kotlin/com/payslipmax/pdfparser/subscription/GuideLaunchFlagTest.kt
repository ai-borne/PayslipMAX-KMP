package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * The Claim Guide ships dark until phase E9 (security review, end-to-end tests, release walkthrough). This
 * fails if the flag is flipped early, so turning the Guide on is a deliberate edit to this test as well.
 */
class GuideLaunchFlagTest {
    @Test
    fun guideStaysDarkInReleaseUntilPhaseE9() {
        assertFalse(LaunchFlags.GUIDE_ENABLED)
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
