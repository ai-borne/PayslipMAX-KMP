package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.ui.theme.InsightsStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WellnessChipLabelTest {
    @Test
    fun wellnessChipLabelIsPayHealth() {
        assertEquals("Pay Health", InsightsStrings.wellnessChipLabel)
    }

    @Test
    fun wellnessChipLabelIsNotBlank() {
        assertTrue(InsightsStrings.wellnessChipLabel.isNotBlank())
    }
}
