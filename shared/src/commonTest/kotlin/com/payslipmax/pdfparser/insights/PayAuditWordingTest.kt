package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals

/** The one wording source for rupees, percentages and months in findings and change reasons (plain words, no raw "1/2026"). */
class PayAuditWordingTest {
    @Test
    fun rupeesUseIndianGroupingWithTheRupeeSign() {
        assertEquals("₹9,870", PayAuditWording.rupees(9870.0))
        assertEquals("₹1,23,100", PayAuditWording.rupees(123100.0))
        assertEquals("₹22,13,800", PayAuditWording.rupees(2213800.0))
        assertEquals("₹500", PayAuditWording.rupees(500.0))
        assertEquals("₹0", PayAuditWording.rupees(0.0))
        assertEquals("-₹5", PayAuditWording.rupees(-5.0))
        assertEquals("₹0", PayAuditWording.rupees(-0.4))
    }

    @Test
    fun percentChangeReadsAsWordsNotAnArrowOrDash() {
        assertEquals("58% to 60%", PayAuditWording.percentChange(58, 60))
    }

    @Test
    fun monthsAreNamedNotNumbered() {
        assertEquals("January 2026", PayAuditWording.monthYear(1, 2026))
        assertEquals("March 2026", PayAuditWording.monthYear(3, 2026))
    }

    @Test
    fun aSpanNamesBothEndsAndTheYearOnce() {
        assertEquals("July 2018", PayAuditWording.monthSpan(7, 7, 2018))
        assertEquals("January to March 2026", PayAuditWording.monthSpan(1, 3, 2026))
    }
}
