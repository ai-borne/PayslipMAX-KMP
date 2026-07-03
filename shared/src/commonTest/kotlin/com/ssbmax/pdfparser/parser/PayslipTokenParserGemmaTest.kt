package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.FieldSource
import com.ssbmax.pdfparser.insights.gemma.GemmaEngineConfig
import com.ssbmax.pdfparser.insights.gemma.MockGemmaEngine
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun applyGemmaFallbackTagsMergedFieldsAsGemmaFallbackNotGeometry() {
        val mockEngine = MockGemmaEngine(config = testConfig)
        mockEngine.mockResponse =
            """
            {
              "earnings": { "specialAllowance": 1200.0 },
              "deductions": { "incomeTax": 400.0 }
            }
            """.trimIndent()
        val extractor = GemmaFallbackExtractor(mockEngine = mockEngine)

        // A solved table with genuine raw leftovers (unmatched tokens), as ReconciliationSolver.solve()
        // would produce it, and every already-solved field tagged GEOMETRY.
        val solved =
            SolvedTable(
                earningsMap = mapOf("basicPay" to 100000.0),
                deductionsMap = emptyMap(),
                rawEarnings = mapOf("SPL ALLW" to 1200.0),
                rawDeductions = mapOf("ITAX" to 400.0),
                reconciled =
                    ReconciledTotals(
                        realGross = 101200.0,
                        realDeductions = 400.0,
                        finalNet = 100800.0,
                        miscEarnings = 0.0,
                        miscDeductions = 0.0,
                        ledger = LedgerCarryOver(0.0, 0.0, 0.0, 0.0),
                    ),
                fieldConfidence = mapOf("basicPay" to 1.0f, "SPL ALLW" to 0.8f, "ITAX" to 0.8f),
                fieldSource = mapOf("basicPay" to FieldSource.GEOMETRY, "SPL ALLW" to FieldSource.GEOMETRY, "ITAX" to FieldSource.GEOMETRY),
                needsReview = true,
            )

        val merged = applyGemmaFallback(solved, extractor)

        assertEquals(1200.0, merged.earningsMap["specialAllowance"])
        assertEquals(400.0, merged.deductionsMap["incomeTax"])
        assertEquals(
            FieldSource.GEMMA_FALLBACK,
            merged.fieldSource["specialAllowance"],
            "a field Gemma recovered from raw leftovers must be provenance-tagged GEMMA_FALLBACK",
        )
        assertEquals(
            FieldSource.GEMMA_FALLBACK,
            merged.fieldSource["incomeTax"],
        )
        assertEquals(
            FieldSource.GEOMETRY,
            merged.fieldSource["basicPay"],
            "a field the geometry solver already had must keep its GEOMETRY tag untouched by the merge",
        )
    }
}
