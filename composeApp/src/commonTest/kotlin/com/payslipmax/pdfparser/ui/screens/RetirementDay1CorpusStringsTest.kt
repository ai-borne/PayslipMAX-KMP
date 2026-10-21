package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The leave row names the days its amount is for; a fixed "300 Days" was wrong whenever fewer days were entered. The basis line names
 * the month whose pay the figures use, for the same reason.
 */
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

    @Test
    fun basisLabelNamesNpaBecauseItCountsTowardPensionGratuityAndLeave() {
        assertEquals("Based on Dec 2024 pay (Basic + MSP + NPA, if any, + DA)", AppStringsPremium.retCalcBasisLabel("Dec 2024"))
    }

    @Test
    fun basisLabelNamesTheSelectedMonthNotTheLatest() {
        // Item 8: the Day-1 figures are priced at the selected payslip's pay. On an older month "current pay" was wrong, and it sat
        // above a Leave bank card captioned with the latest month, so the line must name the month it actually uses.
        assertEquals(
            "Based on Dec 2024 pay (Basic + MSP + NPA, if any, + DA)",
            retCalcBasisLabel(SyntheticLeaveAccumulation.payslip(2024, 12)),
        )
        assertEquals(
            "Based on Sep 2026 pay (Basic + MSP + NPA, if any, + DA)",
            retCalcBasisLabel(SyntheticLeaveAccumulation.payslip(2026, 9)),
        )
    }
}
