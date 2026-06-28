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
}
