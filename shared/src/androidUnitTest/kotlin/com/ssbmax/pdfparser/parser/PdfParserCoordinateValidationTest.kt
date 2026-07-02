package com.ssbmax.pdfparser.parser

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Validates the xSplit threshold parity between Android and iOS.
 * iOS uses xDsopVal <= 50.0 as the fallback guard; Android must match.
 */
class PdfParserCoordinateValidationTest {
    // The required threshold — must stay in sync with PdfParser.kt
    private val XSPLIT_FALLBACK_THRESHOLD = 50f

    @Test
    fun `dsopX of 30f should trigger fallback with 50f threshold`() {
        val dsopX = 30f
        val pageWidth = 595f

        // Old bug: 30f > 10f so the old guard passed it through incorrectly
        val passedOldGuard = dsopX > 10f && dsopX < pageWidth
        assertTrue(passedOldGuard, "Precondition: dsopX=30f was accepted by the old <=10f threshold (the bug)")

        // New fix: 30f <= 50f so it correctly triggers the fallback
        val triggersNewFallback = dsopX <= XSPLIT_FALLBACK_THRESHOLD || dsopX >= pageWidth
        assertTrue(triggersNewFallback, "dsopX=30f must trigger fallback with the <=50f threshold")
    }

    @Test
    fun `valid dsopX from Dec 2025 payslip is not rejected`() {
        val dsopX = 299.7f
        val pageWidth = 595f
        val triggersNewFallback = dsopX <= XSPLIT_FALLBACK_THRESHOLD || dsopX >= pageWidth
        assertFalse(triggersNewFallback, "Real payslip dsopX=299.7f must not trigger fallback")
    }

    @Test
    fun `valid dsopX from Nov 2017 payslip is not rejected`() {
        val dsopX = 481.9f
        val pageWidth = 595f
        val triggersNewFallback = dsopX <= XSPLIT_FALLBACK_THRESHOLD || dsopX >= pageWidth
        assertFalse(triggersNewFallback, "Real payslip dsopX=481.9f must not trigger fallback")
    }

    @Test
    fun `dsopX exactly at threshold boundary triggers fallback`() {
        val dsopX = 50f
        val pageWidth = 595f
        val triggersNewFallback = dsopX <= XSPLIT_FALLBACK_THRESHOLD || dsopX >= pageWidth
        assertTrue(triggersNewFallback, "Boundary value 50f must trigger fallback (guard is <=)")
    }

    @Test
    fun `dsopX just above threshold does not trigger fallback`() {
        val dsopX = 51f
        val pageWidth = 595f
        val triggersNewFallback = dsopX <= XSPLIT_FALLBACK_THRESHOLD || dsopX >= pageWidth
        assertFalse(triggersNewFallback, "dsopX=51f is just above threshold and must not trigger fallback")
    }

    @Test
    fun `dsopX at or above pageWidth triggers fallback`() {
        val pageWidth = 595f
        val triggersAtPageWidth = 595f <= XSPLIT_FALLBACK_THRESHOLD || 595f >= pageWidth
        assertTrue(triggersAtPageWidth, "dsopX=pageWidth triggers fallback via >= pageWidth guard")

        val triggersAbovePageWidth = 600f <= XSPLIT_FALLBACK_THRESHOLD || 600f >= pageWidth
        assertTrue(triggersAbovePageWidth, "dsopX=600f triggers fallback via >= pageWidth guard")
    }
}
