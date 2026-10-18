package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The TA/DA pre-claim worksheet is on since plan 22, P9 (2026-10-18), after the device and iPhone walks. This fails if the flag is
 * switched off again, so pulling the worksheet is a deliberate edit to this test as well.
 */
class PreClaimLaunchFlagTest {
    @Test
    fun preClaimIsOnFromTheReleaseStepP9() {
        assertTrue(LaunchFlags.PRECLAIM_ENABLED)
    }
}
