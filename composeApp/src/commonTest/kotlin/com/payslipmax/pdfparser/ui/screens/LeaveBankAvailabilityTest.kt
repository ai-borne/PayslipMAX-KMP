package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.subscription.LaunchFlags
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every Leave Bank surface hides behind the kill switch and nothing else; the parser runs either way. */
class LeaveBankAvailabilityTest {
    @Test
    fun leaveBankVisibilityIsExactlyTheKillSwitch() {
        assertEquals(LaunchFlags.LEAVE_BANK_ENABLED, isLeaveBankEnabled())
    }
}
