package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class LedgerCorrectionDialogReasonTest {
    @Test
    fun testNetResidualFormatting() {
        val original = "Net residual 24229.0 >= NET_TOLERANCE (2.0)"
        val expected = "The Take-Home pay does not match the difference between total earnings and deductions."
        assertEquals(expected, formatReviewReason(original))
    }

    @Test
    fun testLowConfidenceFieldsFormatting() {
        val original = "Low confidence fields (< 0.7): basicPay, dearnessAllowance"
        val expected = "Verify these low confidence entries: basicPay, dearnessAllowance"
        assertEquals(expected, formatReviewReason(original))
    }

    @Test
    fun testMissingMandatoryCreditsFormatting() {
        val original = "Missing mandatory credits: basicPay"
        val expected = "Expected earnings entries not detected: basicPay"
        assertEquals(expected, formatReviewReason(original))
    }

    @Test
    fun testMissingMandatoryDebitsFormatting() {
        val original = "Missing mandatory debits: dsopSubscription"
        val expected = "Expected deductions entries not detected: dsopSubscription"
        assertEquals(expected, formatReviewReason(original))
    }

    @Test
    fun testSchemaValidationFailureFormatting() {
        val original = "Schema validation failed (post Tier 6): gross mismatch 2018.0, deductions mismatch 137729.0, net residual 115415.0"
        val expected = "The sum of items does not match the printed Gross Pay or Total Deductions."
        assertEquals(expected, formatReviewReason(original))
    }

    @Test
    fun testUnrecognizedReasonFormattingIsNoOp() {
        val original = "Some other unrecognized error message."
        assertEquals(original, formatReviewReason(original))
    }
}
