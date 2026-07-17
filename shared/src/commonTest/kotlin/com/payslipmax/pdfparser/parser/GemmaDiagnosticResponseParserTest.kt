package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemmaDiagnosticResponseParserTest {
    private val allowedKeys = setOf("basicPay", "dearnessAllowance", "incomeTax")

    @Test
    fun testParseValidJsonResponse() {
        val json = """{"fieldKey": "basicPay", "reason": "Off by ~50000, likely swapped with an adjacent row"}"""

        val result = GemmaDiagnosticResponseParser.parse(json, allowedKeys)

        assertEquals("basicPay", result?.fieldKey)
        assertEquals("Off by ~50000, likely swapped with an adjacent row", result?.reason)
    }

    @Test
    fun testParseJsonWithMarkdownBlocks() {
        val json =
            """
            ```json
            {"fieldKey": "incomeTax", "reason": "Deduction total does not match residual"}
            ```
            """.trimIndent()

        val result = GemmaDiagnosticResponseParser.parse(json, allowedKeys)

        assertEquals("incomeTax", result?.fieldKey)
    }

    @Test
    fun testParseInvalidJsonReturnsNull() {
        val result = GemmaDiagnosticResponseParser.parse("This is not JSON", allowedKeys)

        assertNull(result)
    }

    @Test
    fun testParseUnknownFieldKeyReturnsNull() {
        val json = """{"fieldKey": "notARealField", "reason": "some reason"}"""

        val result = GemmaDiagnosticResponseParser.parse(json, allowedKeys)

        assertNull(result)
    }

    @Test
    fun testParseExtraJsonKeyFailsClosed() {
        val json =
            """{"fieldKey": "basicPay", "reason": "off by 50000", "correctedValue": 12345}"""

        val result = GemmaDiagnosticResponseParser.parse(json, allowedKeys)

        assertNull(result)
    }

    @Test
    fun testParseOverLengthReasonIsTruncated() {
        val longReason = "x".repeat(500)
        val json = """{"fieldKey": "basicPay", "reason": "$longReason"}"""

        val result = GemmaDiagnosticResponseParser.parse(json, allowedKeys)

        assertEquals(200, result?.reason?.length)
        assertTrue(result?.reason?.all { it == 'x' } == true)
    }

    @Test
    fun testParseEmptyResponseReturnsNull() {
        val result = GemmaDiagnosticResponseParser.parse("", allowedKeys)

        assertNull(result)
    }

    @Test
    fun testParseNoSuspectFieldResponseReturnsNull() {
        val result = GemmaDiagnosticResponseParser.parse("{}", allowedKeys)

        assertNull(result)
    }
}
