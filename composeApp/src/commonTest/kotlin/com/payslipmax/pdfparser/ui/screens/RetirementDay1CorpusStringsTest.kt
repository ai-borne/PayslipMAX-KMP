package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import kotlin.test.Test
import kotlin.test.assertEquals

/** The leave row names the days its amount is for; a fixed "300 Days" was wrong whenever fewer days were entered. */
class RetirementDay1CorpusStringsTest {
    @Test
    fun leaveEncashmentLabelNamesTheDaysUsed() {
        assertEquals("Leave Encashment (222 Days)", AppStringsPremium.retDay1LeaveEncashmentLabel(222))
        assertEquals("Leave Encashment (300 Days)", AppStringsPremium.retDay1LeaveEncashmentLabel(300))
        assertEquals("Leave Encashment (1 Day)", AppStringsPremium.retDay1LeaveEncashmentLabel(1))
    }

    @Test
    fun leaveHintStatesWhatPcdaCountsAndExcludes() {
        // Owner-approved wording of the Handbook P&A 2023 ch. 29 rule: MSP and the DA on it are both left out.
        assertEquals(
            "(Basic + NPA + DA on them) ÷ 30 × days. MSP and DA on MSP are excluded per PCDA.",
            AppStringsPremium.retCalcLeaveHint,
        )
    }
}
