package com.payslipmax.pdfparser.subscription

import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * The TA/DA pre-claim worksheet is a dark launch: built, tested and shipped in the bundle, but no user can reach it until the owner
 * flips the flag in the release step (plan 22, P9). This fails if the flag is on, so turning it on is a deliberate edit to this test
 * in the same commit, after the device walks (P8).
 */
class PreClaimLaunchFlagTest {
    @Test
    fun preClaimStaysDarkUntilTheOwnerFlipsItInTheReleaseStep() {
        assertFalse(LaunchFlags.PRECLAIM_ENABLED)
    }
}
