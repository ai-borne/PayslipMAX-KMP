package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.payAuditPayslip
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * DA arrears land on whichever payslip follows the order, not only inside the half-year of the rise. DoE OM 1/2(i)/2026-E.II(B):
 * DA 60% to 63% effective 1 Jul 2026. The explanation must name the months the payment actually covers.
 */
class PayLineChangeLateArrearsTest {
    private fun reason(
        before: ParsedPayslip,
        after: ParsedPayslip,
    ): String? {
        val history = listOf(before, after)
        return PayLineChangeExplainer.explain(after, history, ServiceTimelineBuilder.build(history)).single { it.field == "arrearsDa" }.reason
    }

    @Test
    fun arrearsPaidInJanuaryForTheLastHalfYearNameJulyToDecember() {
        // 156000 * 3% * 6 months = 28080
        val text =
            reason(
                payAuditPayslip(2026, 12, 140500.0, daPercent = 60.0),
                payAuditPayslip(2027, 1, 140500.0, daPercent = 63.0, arrearsDa = 28080.0),
            )
        assertEquals("Dearness Allowance (DA) rate changed from 60% to 63%; these are the arrears for July to December 2026", text)
    }

    @Test
    fun arrearsPaidInFebruaryRunIntoTheNextYear() {
        // 156000 * 3% * 7 months = 32760
        val text =
            reason(
                payAuditPayslip(2027, 1, 140500.0, daPercent = 60.0),
                payAuditPayslip(2027, 2, 140500.0, daPercent = 63.0, arrearsDa = 32760.0),
            )
        assertEquals("Dearness Allowance (DA) rate changed from 60% to 63%; these are the arrears for July 2026 to January 2027", text)
    }
}
