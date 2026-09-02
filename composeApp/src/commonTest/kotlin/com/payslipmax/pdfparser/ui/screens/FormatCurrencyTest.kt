package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatCurrencyTest {
    @Test
    fun zero_formats_without_negative_sign() {
        assertEquals("₹0", formatCurrency(0.0))
        assertEquals("₹0", formatCurrency(-0.0))
    }

    @Test
    fun single_and_double_digit_formats_correctly() {
        assertEquals("₹5", formatCurrency(5.0))
        assertEquals("-₹5", formatCurrency(-5.0))
        assertEquals("₹50", formatCurrency(50.0))
        assertEquals("-₹50", formatCurrency(-50.0))
    }

    @Test
    fun three_digit_values_format_without_glitch() {
        assertEquals("₹100", formatCurrency(100.0))
        assertEquals("-₹100", formatCurrency(-100.0))
        assertEquals("₹250", formatCurrency(250.0))
        assertEquals("-₹250", formatCurrency(-250.0))
        assertEquals("₹999", formatCurrency(999.0))
        assertEquals("-₹999", formatCurrency(-999.0))
    }

    @Test
    fun four_and_five_digit_thousands_format_correctly() {
        assertEquals("₹1,000", formatCurrency(1000.0))
        assertEquals("-₹1,000", formatCurrency(-1000.0))
        assertEquals("₹1,250", formatCurrency(1250.0))
        assertEquals("-₹1,250", formatCurrency(-1250.0))
        assertEquals("₹50,000", formatCurrency(50000.0))
        assertEquals("-₹50,000", formatCurrency(-50000.0))
    }

    @Test
    fun lakhs_and_crores_format_with_indian_numbering_system() {
        assertEquals("₹1,00,000", formatCurrency(100000.0))
        assertEquals("-₹1,00,000", formatCurrency(-100000.0))
        assertEquals("₹2,50,000", formatCurrency(250000.0))
        assertEquals("-₹2,50,000", formatCurrency(-250000.0))
        assertEquals("₹1,00,00,000", formatCurrency(10000000.0))
        assertEquals("-₹1,00,00,000", formatCurrency(-10000000.0))
        assertEquals("₹1,23,45,678", formatCurrency(12345678.0))
        assertEquals("-₹1,23,45,678", formatCurrency(-12345678.0))
    }
}
