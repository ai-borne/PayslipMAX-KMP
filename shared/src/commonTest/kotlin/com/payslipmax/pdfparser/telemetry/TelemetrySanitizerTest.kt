package com.payslipmax.pdfparser.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelemetrySanitizerTest {
    @Test
    fun isKeyAllowed_allowsStandardDiagnosticKeys() {
        assertTrue(TelemetrySanitizer.isKeyAllowed("app_version"))
        assertTrue(TelemetrySanitizer.isKeyAllowed("parser_version"))
        assertTrue(TelemetrySanitizer.isKeyAllowed("screen"))
        assertTrue(TelemetrySanitizer.isKeyAllowed("operation"))
        assertTrue(TelemetrySanitizer.isKeyAllowed("format_detected"))
        assertTrue(TelemetrySanitizer.isKeyAllowed("page_count"))
        assertTrue(TelemetrySanitizer.isKeyAllowed("duration_ms"))
    }

    @Test
    fun isKeyAllowed_blocksPiiAndFinancialKeys() {
        assertFalse(TelemetrySanitizer.isKeyAllowed("pan_number"))
        assertFalse(TelemetrySanitizer.isKeyAllowed("cda_account"))
        assertFalse(TelemetrySanitizer.isKeyAllowed("salary_amount"))
        assertFalse(TelemetrySanitizer.isKeyAllowed("officer_name"))
        assertFalse(TelemetrySanitizer.isKeyAllowed("bpay"))
        assertFalse(TelemetrySanitizer.isKeyAllowed("tax_deduction"))
        assertFalse(TelemetrySanitizer.isKeyAllowed("app_pin"))
    }

    @Test
    fun sanitizeMessage_redactsPanCardNumbers() {
        val input = "Processing file for user ABCDE1234F completed"
        val expected = "Processing file for user [REDACTED_PAN] completed"
        assertEquals(expected, TelemetrySanitizer.sanitizeMessage(input))
    }

    @Test
    fun sanitizeMessage_redactsCurrencyAmounts() {
        val input = "Parsed allowance with value ₹ 1,50,000 and Rs. 500"
        val expected = "Parsed allowance with value [REDACTED_AMOUNT] and [REDACTED_AMOUNT]"
        assertEquals(expected, TelemetrySanitizer.sanitizeMessage(input))
    }

    @Test
    fun redactAccountNumbers_redactsNineOrMoreDigitRuns() {
        // A CDA / bank account number typed into a bug report must not leave the device.
        val input = "My CDA account 123456789 and bank 50100123456789 are wrong"
        val expected = "My CDA account [REDACTED_ACCOUNT] and bank [REDACTED_ACCOUNT] are wrong"
        assertEquals(expected, TelemetrySanitizer.redactAccountNumbers(input))
    }

    @Test
    fun redactAccountNumbers_leavesShortNumbersUntouched() {
        // 4-digit ranks/PINs, years and 8-digit dates are useful triage context, not account numbers.
        val input = "Rank 1234, year 2026, date 20260920, error 404"
        assertEquals(input, TelemetrySanitizer.redactAccountNumbers(input))
    }

    @Test
    fun sanitizeMessage_doesNotRedactAccountNumbers() {
        // Crashlytics breadcrumbs share sanitizeMessage(); scrubbing long digit runs there would
        // wipe legitimate timestamps and object IDs from crash logs app-wide.
        val input = "Failed at 1758355200000 for object 123456789012"
        assertEquals(input, TelemetrySanitizer.sanitizeMessage(input))
    }

    @Test
    fun sanitizeMetadata_filtersKeysAndSanitizesValues() {
        val input =
            mapOf(
                "parser_version" to "v4.2",
                "officer_name" to "Major Smith",
                "operation" to "parse_pdf",
                "salary_total" to "₹ 1,20,000",
            )
        val sanitized = TelemetrySanitizer.sanitizeMetadata(input)
        assertEquals(2, sanitized.size)
        assertEquals("v4.2", sanitized["parser_version"])
        assertEquals("parse_pdf", sanitized["operation"])
        assertFalse(sanitized.containsKey("officer_name"))
        assertFalse(sanitized.containsKey("salary_total"))
    }

    @Test
    fun sanitizeFilename_replacesPiiFilenameWithDeterministicHash() {
        val raw = "Sunil_Pawar_Jan2025_Payslip.pdf"
        val sanitized = TelemetrySanitizer.sanitizeFilename(raw)
        assertTrue(sanitized.startsWith("file_"))
        assertTrue(sanitized.endsWith(".pdf"))
        assertFalse(sanitized.contains("Sunil"))
        assertFalse(sanitized.contains("Pawar"))
        // Deterministic
        assertEquals(sanitized, TelemetrySanitizer.sanitizeFilename(raw))
    }
}
