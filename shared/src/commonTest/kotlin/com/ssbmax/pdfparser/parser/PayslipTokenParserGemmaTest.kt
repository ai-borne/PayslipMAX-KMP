package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.insights.gemma.GemmaEngineConfig
import com.ssbmax.pdfparser.insights.gemma.MockGemmaEngine
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PayslipTokenParserGemmaTest {
    private val testConfig = GemmaEngineConfig(modelPath = "models/gemma3-1b.task")

    @Test
    fun testParseWithFallbackExtractor() {
        val mockEngine = MockGemmaEngine(config = testConfig)
        mockEngine.mockResponse =
            """
            {
              "earnings": { "specialAllowance": 1500.0 },
              "deductions": {}
            }
            """.trimIndent()
        val extractor = GemmaFallbackExtractor(mockEngine = mockEngine)

        val tokenized =
            TokenizedPayslip(
                fullText = "CDA-123 PAN123 January 2026 Gross Pay: 51500 Total Deductions: 1000 Net Remittance: 50500",
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
            )

        val result = PayslipTokenParser.parse(tokenized, "test.pdf", fallbackExtractor = extractor)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun testGemmaFiresOnMissingMandatoryFields() {
        val mockEngine = MockGemmaEngine(config = testConfig)
        mockEngine.mockResponse =
            """
            {
              "earnings": { "militaryServicePay": 15500.0 },
              "deductions": { "agif": 5000.0 }
            }
            """.trimIndent()
        val extractor = GemmaFallbackExtractor(mockEngine = mockEngine)

        // Tokenized payslip missing militaryServicePay and agif in table tokens, but having raw/needsReview state
        val tokenized =
            TokenizedPayslip(
                fullText = "STATEMENT OF ACCOUNT FOR APR 2026 CDA A/C NO: 16/000/000000X RANK & NAME: Maj Officer Officer Officer PERSONAL NO: IC00000N Gross Pay: 84450 Total Deductions: 25416 Net Remittance: 59034",
                tableTokens =
                    listOf(
                        PositionedToken("Basic Pay", 16f, 600f, 100f, 10f),
                        PositionedToken("22300", 135f, 600f, 50f, 10f),
                        PositionedToken("DSOPF Subn", 168f, 600f, 100f, 10f),
                        PositionedToken("2094", 280f, 600f, 50f, 10f),
                    ),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
            )

        val result = PayslipTokenParser.parse(tokenized, "apr14.pdf", fallbackExtractor = extractor)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertTrue(parsed.needsReview, "Missing mandatory fields should keep needsReview flag true or set field confidence")
    }
}
