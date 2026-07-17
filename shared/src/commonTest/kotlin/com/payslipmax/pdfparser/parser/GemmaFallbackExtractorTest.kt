package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.insights.gemma.GemmaEngineConfig
import com.payslipmax.pdfparser.insights.gemma.MockGemmaEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GemmaFallbackExtractorTest {
    private val testConfig = GemmaEngineConfig(modelPath = "models/gemma3-1b.task")

    @Test
    fun testExtractFallbackWithMockEngineSuccess() =
        runTest {
            val mockEngine = MockGemmaEngine(config = testConfig)
            mockEngine.mockResponse =
                """
                {
                  "earnings": { "specialAllowance": 1200.0 },
                  "deductions": { "incomeTax": 400.0 }
                }
                """.trimIndent()

            val extractor = GemmaFallbackExtractor(mockEngine = mockEngine)
            val rawEarn = mapOf("SPL ALLW" to 1200.0)
            val rawDed = mapOf("ITAX" to 400.0)

            val result = extractor.extractFallback(rawEarn, rawDed)

            assertEquals(1200.0, result.earnings["specialAllowance"])
            assertEquals(400.0, result.deductions["incomeTax"])
        }

    @Test
    fun testExtractFallbackEmptyWhenEngineFails() =
        runTest {
            val mockEngine = MockGemmaEngine(config = testConfig, shouldFail = true)
            val extractor = GemmaFallbackExtractor(mockEngine = mockEngine)

            val result = extractor.extractFallback(mapOf("SPL" to 100.0), emptyMap())

            assertTrue(result.earnings.isEmpty())
            assertTrue(result.deductions.isEmpty())
        }
}
