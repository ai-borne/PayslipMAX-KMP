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
