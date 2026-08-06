package com.payslipmax.pdfparser.parser.pipeline

import com.payslipmax.pdfparser.parser.GrammarAwareParser
import com.payslipmax.pdfparser.parser.PositionedToken
import com.payslipmax.pdfparser.parser.TokenizedPayslip
import com.payslipmax.pdfparser.parser.detection.GrammarFamily
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedParsingPipelineTest {
    @Test
    fun testPipelineExecutesMatchedGrammarStrategy() =
        runTest {
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
                DO2 Details Page 4
                """.trimIndent()

            val mockTokenized =
                TokenizedPayslip(
                    tableTokens = tableTokens,
                    taxTokens = emptyList(),
                    dsopTokens = emptyList(),
                    fullText = fullText,
                )

            val result = GrammarAwareParser.parseWithDiagnostics(mockTokenized, "01 Jan 2024.pdf")
            assertTrue(result.isSuccess)
            val (payslip, report) = result.getOrNull()!!

            assertNotNull(payslip)
            assertEquals(GrammarFamily.PCDA_MODERN_GRID, report.selectedFamily)
            assertTrue(report.isKnownGrammar)
            assertEquals(140500.0, payslip.summary.grossPay)
            assertEquals(40000.0, payslip.summary.totalDeductions)
            assertEquals(100500.0, payslip.summary.netRemittance)
        }

    @Test
    fun testPipelineFallbackOnUnknownGrammar() =
        runTest {
            val mockTokenized =
                TokenizedPayslip(
                    tableTokens = emptyList(),
                    taxTokens = emptyList(),
                    dsopTokens = emptyList(),
                    fullText = "01/2024 Custom Unrecognized Layout without matching keywords\nGross Pay 100 Total Deductions 10 Net Remittance 90",
                )

            val result = GrammarAwareParser.parseWithDiagnostics(mockTokenized, "01 Jan 2024.pdf")
            assertTrue(result.isSuccess)
            val (payslip, report) = result.getOrNull()!!

            assertNotNull(payslip)
            assertEquals(GrammarFamily.UNKNOWN, report.selectedFamily)
            assertFalse(report.isKnownGrammar)
        }

    @Test
    fun testPipelineReconciliationConfidenceIntegration() =
        runTest {
            val tableTokens =
                listOf(
                    PositionedToken("BPAY", 10f, 10f, 30f, 15f),
                    PositionedToken("140500", 50f, 10f, 30f, 15f),
                )
            val fullText =
                """
                01/2024  STATEMENT OF ACCOUNT FOR 01/2024
                Name: Officer Officer Officer A/C No - 16/000/000000X PAN No: AR*****90G
                BPAY 140500
                kuula Aaya Gross Pay 140500 kuula kTaOtI Total Deductions 0
                Net Remittance : Rs.1,40,500
                """.trimIndent()

            val mockTokenized =
                TokenizedPayslip(
                    tableTokens = tableTokens,
                    taxTokens = emptyList(),
                    dsopTokens = emptyList(),
                    fullText = fullText,
                )

            val result = GrammarAwareParser.parse(mockTokenized, "01 Jan 2024.pdf")
            assertTrue(result.isSuccess)
            val payslip = result.getOrNull()!!

            assertNotNull(payslip)
            assertNotNull(payslip.fieldConfidence)
            assertEquals(140500.0, payslip.earnings.basicPay)
        }
}
