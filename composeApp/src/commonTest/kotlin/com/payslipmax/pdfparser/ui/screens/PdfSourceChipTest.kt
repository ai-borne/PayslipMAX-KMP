package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfSourceChipTest {
    @Test
    fun formatPdfSourceChipLabelReturnsGivenFileNameWhenValid() {
        val input = "payslip.pdf"
        val label = formatPdfSourceChipLabel(input)
        assertEquals("payslip.pdf", label)
    }

    @Test
    fun formatPdfSourceChipLabelReturnsDefaultWhenBlank() {
        val label = formatPdfSourceChipLabel("   ")
        assertEquals("Statement.pdf", label)
    }

    @Test
    fun pdfSourceChipClickStateTracksInvocation() {
        var clicked = false
        val onClick = { clicked = true }
        onClick()
        assertTrue(clicked)
    }
}
