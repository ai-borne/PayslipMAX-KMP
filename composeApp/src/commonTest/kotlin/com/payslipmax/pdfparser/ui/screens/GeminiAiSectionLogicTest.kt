package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class GeminiAiSectionLogicTest {
    @Test
    fun testExtractSummaryFromPlainParagraph() {
        val insights = "Your tax situation is healthy. You have ₹50,000 of 80C headroom remaining."
        assertEquals(
            "Your tax situation is healthy. You have ₹50,000 of 80C headroom remaining.",
            extractAiSummary(insights),
        )
    }

    @Test
    fun testExtractSummarySkipsHeaders() {
        val insights =
            """
            # AI CA Audit Report

            ## Executive Summary

            Your payslip for August 2024 shows a net taxable income of ₹5,00,000.
            """.trimIndent()
        assertEquals(
            "Your payslip for August 2024 shows a net taxable income of ₹5,00,000.",
            extractAiSummary(insights),
        )
    }

    @Test
    fun testExtractSummarySkipsSeparators() {
        val insights =
            """
            ---
            Your monthly gross pay is ₹1,00,000.
            """.trimIndent()
        assertEquals("Your monthly gross pay is ₹1,00,000.", extractAiSummary(insights))
    }

    @Test
    fun testExtractSummaryTruncatesToMaxLength() {
        val longLine = "A".repeat(300)
        val result = extractAiSummary(longLine, maxLength = 200)
        assertEquals(201, result.length) // 200 chars + "…"
        assertEquals("…", result.last().toString())
    }

    @Test
    fun testExtractSummaryStripsListMarkers() {
        val insights = "- Your DSOP subscription is below the recommended threshold."
        assertEquals(
            "Your DSOP subscription is below the recommended threshold.",
            extractAiSummary(insights),
        )
    }

    @Test
    fun testExtractSummaryStripsAsteriskListMarkers() {
        val insights = "* Consider increasing your DSOP by ₹2,000 per month."
        assertEquals(
            "Consider increasing your DSOP by ₹2,000 per month.",
            extractAiSummary(insights),
        )
    }

    @Test
    fun testExtractSummaryEmptyInput() {
        assertEquals("", extractAiSummary(""))
    }

    @Test
    fun testExtractSummaryOnlyHeaders() {
        val insights = "# Header\n## Sub\n### Sub-sub"
        assertEquals("", extractAiSummary(insights))
    }

    @Test
    fun testExtractSummaryExactlyMaxLength() {
        val line = "B".repeat(200)
        val result = extractAiSummary(line, maxLength = 200)
        assertEquals(line, result) // no truncation, no "…"
    }
}
