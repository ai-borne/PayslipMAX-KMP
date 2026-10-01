package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayMonth
import kotlin.test.Test
import kotlin.test.assertEquals

/** U3/U5: a change row must name its pay line, and months/amounts must read like a person would write them. */
class PayAuditFormatTest {
    @Test
    fun monthReadsAsShortNameAndYear() {
        assertEquals("Aug 2026", formatPayMonth(PayMonth(2026, 8)))
        assertEquals("Jan 2024", formatPayMonth(PayMonth(2024, 1)))
    }

    @Test
    fun trackedPayLineFieldsAreNamedInPlainWords() {
        assertEquals("TPTA (transport allowance)", payLineLabel("transportAllowance"))
        assertEquals("Dearness Allowance (DA)", payLineLabel("dearnessAllowance"))
        assertEquals("Military Service Pay (MSP)", payLineLabel("militaryServicePay"))
        assertEquals("DA arrears", payLineLabel("arrearsDa"))
        assertEquals("Basic pay", payLineLabel("basicPay"))
    }

    @Test
    fun unknownFieldFallsBackToReadableText() {
        assertEquals("Some Odd Field", payLineLabel("someOddField"))
    }

    @Test
    fun changeAmountsUseIndianGroupingAndNameBothEnds() {
        assertEquals("₹1,49,000 → ₹95,410", formatChangeAmounts(149000.0, 95410.0))
    }
}
