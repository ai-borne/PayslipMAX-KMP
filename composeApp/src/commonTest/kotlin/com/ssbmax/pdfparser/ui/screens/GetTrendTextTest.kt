package com.ssbmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class GetTrendTextTest {
    @Test
    fun testNullDeltaIsEmpty() {
        assertEquals("", getTrendText(null, "May"))
    }

    @Test
    fun testZeroDeltaIsEmpty() {
        assertEquals("", getTrendText(0, "May"))
    }

    @Test
    fun testPositiveDeltaWithMonthLabel() {
        assertEquals("↑ Improved by 8 points since May", getTrendText(8, "May"))
    }

    @Test
    fun testPositiveDeltaWithNullMonthLabelFallsBack() {
        assertEquals("↑ Improved by 8 points since last payslip", getTrendText(8, null))
    }

    @Test
    fun testNegativeDeltaWithMonthLabel() {
        assertEquals("↓ Down 4 points since May", getTrendText(-4, "May"))
    }

    @Test
    fun testNegativeDeltaWithNullMonthLabelFallsBack() {
        assertEquals("↓ Down 4 points since last payslip", getTrendText(-4, null))
    }
}
