package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.insights.gemma.GemmaEngineConfig
import com.payslipmax.pdfparser.insights.gemma.MockGemmaEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GemmaDiagnosticExtractorTest {
    private val testConfig = GemmaEngineConfig(modelPath = "models/gemma3-1b.task")

    @Test
    fun testSuggestDiagnosticReturnsSuggestionFromMockEngine() =
        runTest {
            val mockEngine = MockGemmaEngine(config = testConfig)
            mockEngine.mockResponse = """{"fieldKey": "basicPay", "reason": "Off by ~50000, likely swapped with an adjacent row"}"""
            val extractor = GemmaDiagnosticExtractor(mockEngine = mockEngine)

            val result =
                extractor.suggestDiagnostic(
                    earnings = mapOf("basicPay" to 50000.0),
                    deductions = mapOf("incomeTax" to 5000.0),
                    grossPay = 70000.0,
                    totalDeductions = 5000.0,
                    netRemittance = 15000.0,
                    residual = 50000.0,
                )

            assertEquals("basicPay", result?.fieldKey)
        }

    @Test
    fun testSuggestDiagnosticReturnsNullWhenNoEngineProvided() =
        runTest {
            val extractor = GemmaDiagnosticExtractor()

            val result =
                extractor.suggestDiagnostic(
                    earnings = mapOf("basicPay" to 50000.0),
                    deductions = emptyMap(),
                    grossPay = 50000.0,
                    totalDeductions = 0.0,
                    netRemittance = 40000.0,
                    residual = 10000.0,
                )

            assertNull(result, "no model present must swallow to null, never block or fail the parse")
        }

    @Test
    fun testSuggestDiagnosticReturnsNullWhenEngineThrows() =
        runTest {
            val mockEngine = MockGemmaEngine(config = testConfig, shouldFail = true)
            val extractor = GemmaDiagnosticExtractor(mockEngine = mockEngine)

            val result =
                extractor.suggestDiagnostic(
                    earnings = mapOf("basicPay" to 50000.0),
                    deductions = emptyMap(),
                    grossPay = 50000.0,
                    totalDeductions = 0.0,
                    netRemittance = 40000.0,
                    residual = 10000.0,
                )

            assertNull(result, "a Gemma call failure must be swallowed, not propagated")
        }

    @Test
    fun testSuggestDiagnosticReturnsNullForUnknownFieldKey() =
        runTest {
            val mockEngine = MockGemmaEngine(config = testConfig)
            mockEngine.mockResponse = """{"fieldKey": "notARealField", "reason": "some reason"}"""
            val extractor = GemmaDiagnosticExtractor(mockEngine = mockEngine)

            val result =
                extractor.suggestDiagnostic(
                    earnings = mapOf("basicPay" to 50000.0),
                    deductions = emptyMap(),
                    grossPay = 50000.0,
                    totalDeductions = 0.0,
                    netRemittance = 40000.0,
                    residual = 10000.0,
                )

            assertNull(result, "a fieldKey outside the earnings/deductions SSOT keys must fail closed")
        }
}
