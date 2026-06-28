package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GemmaResponseParserTest {
    @Test
    fun testParseValidJsonResponse() {
        val json =
            """
            {
              "earnings": {
                "specialAllowance": 1500.0
              },
              "deductions": {
                "incomeTax": 300.0
              }
            }
            """.trimIndent()

        val result = GemmaResponseParser.parse(json)

        assertEquals(1500.0, result.earnings["specialAllowance"])
        assertEquals(300.0, result.deductions["incomeTax"])
    }

    @Test
    fun testParseJsonWithMarkdownBlocks() {
        val json =
            """
            ```json
            {
              "earnings": {
                "dearnessAllowance": 2500.0
              },
              "deductions": {}
            }
            ```
            """.trimIndent()

        val result = GemmaResponseParser.parse(json)

        assertEquals(2500.0, result.earnings["dearnessAllowance"])
        assertTrue(result.deductions.isEmpty())
    }

    @Test
    fun testParseInvalidJsonReturnsEmptyMaps() {
        val invalidJson = "This is not a JSON response"

        val result = GemmaResponseParser.parse(invalidJson)

        assertTrue(result.earnings.isEmpty())
        assertTrue(result.deductions.isEmpty())
    }
}
