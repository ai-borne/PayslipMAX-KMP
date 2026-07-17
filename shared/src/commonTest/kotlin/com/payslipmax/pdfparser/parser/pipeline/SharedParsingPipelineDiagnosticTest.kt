package com.payslipmax.pdfparser.parser.pipeline

import com.payslipmax.pdfparser.insights.gemma.GemmaEngineConfig
import com.payslipmax.pdfparser.insights.gemma.MockGemmaEngine
import com.payslipmax.pdfparser.parser.GemmaDiagnosticExtractor
import com.payslipmax.pdfparser.parser.GrammarAwareParser
import com.payslipmax.pdfparser.parser.PositionedToken
import com.payslipmax.pdfparser.parser.TokenizedPayslip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tier 6 diagnostic pass wiring into [SharedParsingPipeline]/[GrammarAwareParser]. The named
 * regression case reproduces the shape of the real ~₹128,370 residual observed on a
 * `PCDA_EARLY_DUAL_COL` payslip during the on-device smoke test that motivated this feature
 * (synthetic numbers only, no real PII).
 */
class SharedParsingPipelineDiagnosticTest {
    private val testConfig = GemmaEngineConfig(modelPath = "models/gemma3-1b.task")

    private fun reconciledTokenized(): TokenizedPayslip {
        val tableTokens =
            listOf(
                PositionedToken("BPAY", 10f, 10f, 30f, 15f),
                PositionedToken("140500", 50f, 10f, 30f, 15f),
                PositionedToken("DSOP", 10f, 30f, 30f, 15f),
                PositionedToken("40000", 50f, 30f, 30f, 15f),
            )
        val fullText =
            """
            01/2024  STATEMENT OF ACCOUNT FOR 01/2024
            Name: Officer Officer Officer A/C No - 16/000/000000X PAN No: AR*****90G
            BPAY 140500 DSOP 40000
            kuula Aaya Gross Pay 140500 kuula kTaOtI Total Deductions 40000
            Net Remittance : Rs.1,00,500
            """.trimIndent()
        return TokenizedPayslip(tableTokens = tableTokens, taxTokens = emptyList(), dsopTokens = emptyList(), fullText = fullText)
    }

    private fun mismatchedTokenized(): TokenizedPayslip {
        // Same BPAY/DSOP table as reconciledTokenized, but Gross Pay is overstated by 128370 —
        // the incident's residual magnitude — while Total Deductions/Net Remittance stay internally
        // consistent, so only grossMismatch (not deductionsMismatch/netResidual) trips Tier 7.
        val tableTokens =
            listOf(
                PositionedToken("BPAY", 10f, 10f, 30f, 15f),
                PositionedToken("140500", 50f, 10f, 30f, 15f),
                PositionedToken("DSOP", 10f, 30f, 30f, 15f),
                PositionedToken("40000", 50f, 30f, 30f, 15f),
            )
        val fullText =
            """
            01/2024  STATEMENT OF ACCOUNT FOR 01/2024
            Name: Officer Officer Officer A/C No - 16/000/000000X PAN No: AR*****90G
            BPAY 140500 DSOP 40000
            kuula Aaya Gross Pay 268870 kuula kTaOtI Total Deductions 40000
            Net Remittance : Rs.2,28,870
            """.trimIndent()
        return TokenizedPayslip(tableTokens = tableTokens, taxTokens = emptyList(), dsopTokens = emptyList(), fullText = fullText)
    }

    @Test
    fun testDiagnosticStaysNullWhenSchemaValid() {
        val mockEngine = MockGemmaEngine(config = testConfig)
        mockEngine.mockResponse = """{"fieldKey": "basicPay", "reason": "would fire if the extractor were wrongly invoked"}"""
        val diagnosticExtractor = GemmaDiagnosticExtractor(mockEngine = mockEngine)

        val result = GrammarAwareParser.parse(reconciledTokenized(), "01 Jan 2024.pdf", diagnosticExtractor = diagnosticExtractor)

        val payslip = result.getOrThrow()
        assertNull(
            payslip.diagnosticSuggestion,
            "Tier 6 diagnostic must only fire on schema-validation failure; a reconciled payslip must never invoke it",
        )
    }

    @Test
    fun testDiagnosticSuggestionPopulatedOnSchemaMismatch() {
        val mockEngine = MockGemmaEngine(config = testConfig)
        mockEngine.mockResponse = """{"fieldKey": "basicPay", "reason": "Gross pay residual of ~128370 suggests basicPay was under-extracted"}"""
        val diagnosticExtractor = GemmaDiagnosticExtractor(mockEngine = mockEngine)

        val result = GrammarAwareParser.parse(mismatchedTokenized(), "01 Jan 2024.pdf", diagnosticExtractor = diagnosticExtractor)

        val payslip = result.getOrThrow()
        assertTrue(payslip.needsReview)
        assertEquals("basicPay", payslip.diagnosticSuggestion?.fieldKey)
        assertEquals(
            "Gross pay residual of ~128370 suggests basicPay was under-extracted",
            payslip.diagnosticSuggestion?.reason,
        )
    }

    @Test
    fun testDiagnosticStaysNullWhenNoExtractorProvided() {
        val result = GrammarAwareParser.parse(mismatchedTokenized(), "01 Jan 2024.pdf")

        val payslip = result.getOrThrow()
        assertTrue(payslip.needsReview, "the parse itself must still succeed and flag review, even with no Tier 6 diagnostic available")
        assertNull(payslip.diagnosticSuggestion, "no model present must never block or fail the parse")
    }

    @Test
    fun testDiagnosticSuggestionNullWhenGemmaCallFailsOnMismatch() {
        val mockEngine = MockGemmaEngine(config = testConfig, shouldFail = true)
        val diagnosticExtractor = GemmaDiagnosticExtractor(mockEngine = mockEngine)

        val result = GrammarAwareParser.parse(mismatchedTokenized(), "01 Jan 2024.pdf", diagnosticExtractor = diagnosticExtractor)

        val payslip = result.getOrThrow()
        assertTrue(payslip.needsReview, "a failed diagnostic call must not affect the rest of the parsed payslip")
        assertNull(payslip.diagnosticSuggestion)
        assertEquals(140500.0, payslip.earnings.basicPay, "the geometry-solved fields must be untouched by the diagnostic failure")
    }
}
