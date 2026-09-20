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
    fun redactAccountNumbers_redactsSpaceOrHyphenSeparatedFourDigitGroups() {
        // Account/card numbers are often typed in 4-digit groups, which the contiguous rule misses.
        assertEquals("acct [REDACTED_ACCOUNT] failed", TelemetrySanitizer.redactAccountNumbers("acct 1234 5678 9012 failed"))
        assertEquals("acct [REDACTED_ACCOUNT]", TelemetrySanitizer.redactAccountNumbers("acct 1234-5678-9012-3456"))
    }

    @Test
    fun redactAccountNumbers_leavesDatesAndTimesAlone() {
        // Guard against over-redaction: bug reports routinely quote dates and times.
        val input = "Failed on 2026-09-20 at 10 32 and again 20-09-2026 10 45"
        assertEquals(input, TelemetrySanitizer.redactAccountNumbers(input))
    }

    @Test
    fun redactEmailAddresses_redactsAddressesAndKeepsSurroundingText() {
        assertEquals(
            "contact [REDACTED_EMAIL] please",
            TelemetrySanitizer.redactEmailAddresses("contact first.last+army@mail.example.co.in please"),
        )
        assertEquals("no address here @ all", TelemetrySanitizer.redactEmailAddresses("no address here @ all"))
    }

    @Test
    fun sanitizeFreeText_appliesPanAmountAccountAndEmailRules() {
        val out = TelemetrySanitizer.sanitizeFreeText("PAN ABCDE1234F; Rs. 50,000; acct 1234 5678 9012; me@x.in")
        assertEquals("PAN [REDACTED_PAN]; [REDACTED_AMOUNT]; acct [REDACTED_ACCOUNT]; [REDACTED_EMAIL]", out)
    }

    @Test
    fun sanitizeMessage_doesNotRedactEmailsOrGroupedDigits() {
        // Crash telemetry path must stay untouched by the free-text rules.
        val input = "user me@x.in code 1234 5678 9012"
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
