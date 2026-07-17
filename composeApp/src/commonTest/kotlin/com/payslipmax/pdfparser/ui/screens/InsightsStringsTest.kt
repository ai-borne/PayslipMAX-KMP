package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsStringsTest {
    @Test
    fun wellnessChipLabelIsPayHealth() {
        assertEquals("Pay Health", InsightsStrings.wellnessChipLabel)
    }

    @Test
    fun wellnessChipLabelIsNotBlank() {
        assertTrue(InsightsStrings.wellnessChipLabel.isNotBlank())
    }
}
