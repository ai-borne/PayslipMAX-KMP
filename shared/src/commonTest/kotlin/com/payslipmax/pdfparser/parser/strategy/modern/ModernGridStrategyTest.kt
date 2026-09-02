package com.payslipmax.pdfparser.parser.strategy.modern

import com.payslipmax.pdfparser.parser.GrammarAwareParser
import com.payslipmax.pdfparser.parser.PositionedToken
import com.payslipmax.pdfparser.parser.TokenizedPayslip
import com.payslipmax.pdfparser.parser.detection.GrammarFamily
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModernGridStrategyTest {
    @Test
    fun testApr2026WithoutArrearsResolvesExtendedGrid() =
        runTest {
            // Apr 2026 is Extended-Grid era by statement period even though this month has no ARR-
            // arrears line item; the incidental-text detector alone used to misclassify this as Modern
            // Grid (same failure shape as the real-world Mar 2025 bug this fix addresses).
            val tableTokens =
                listOf(
                    PositionedToken("BPAY", 10f, 10f, 30f, 15f),
                    PositionedToken("(12A)", 45f, 10f, 30f, 15f),
                    PositionedToken("149000", 80f, 10f, 40f, 15f),
                    PositionedToken("DA", 10f, 30f, 20f, 15f),
                    PositionedToken("98700", 80f, 30f, 40f, 15f),
                )
            val fullText =
                """
                04/2026 STATEMENT OF ACCOUNT FOR 04/2026
                Name: Officer Officer Officer A/C No: 16/000/000000X PAN No: AR*****90G
                BPAY (12A) 149000 DA 98700
                kuula Aaya Gross Pay 247700 kuula kTaOtI Total Deductions 0
                Net Remittance : Rs.2,47,700
                DO2 Details Page 4
                """.trimIndent()

            val mockTokenized =
                TokenizedPayslip(
                    tableTokens = tableTokens,
                    taxTokens = emptyList(),
                    dsopTokens = emptyList(),
                    fullText = fullText,
                )

            val result = GrammarAwareParser.parseWithDiagnostics(mockTokenized, "04 Apr 2026.pdf")
            assertTrue(result.isSuccess)
            val (payslip, report) = result.getOrNull()!!

            assertNotNull(payslip)
            assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, report.selectedFamily)
            assertTrue(report.isKnownGrammar)
            assertEquals("ModernGridHeaderStrategy", report.selectedStrategies["HeaderStrategy"])
            assertEquals(247700.0, payslip.summary.grossPay)
        }

    @Test
    fun testExtendedGridStrategyExecution() =
        runTest {
            val tableTokens =
                listOf(
                    PositionedToken("BPAY", 10f, 10f, 30f, 15f),
                    PositionedToken("149000", 50f, 10f, 40f, 15f),
                    PositionedToken("ARR-DA", 10f, 30f, 40f, 15f),
                    PositionedToken("9870", 60f, 30f, 30f, 15f),
                )
            val fullText =
                """
                04/2026 STATEMENT OF ACCOUNT FOR 04/2026
                BPAY 149000 ARR-DA 9870
                Gross Pay 158870 Total Deductions 0 Net Remittance 158870
                """.trimIndent()

            val mockTokenized =
                TokenizedPayslip(
                    tableTokens = tableTokens,
                    taxTokens = emptyList(),
                    dsopTokens = emptyList(),
                    fullText = fullText,
                )

            val result = GrammarAwareParser.parseWithDiagnostics(mockTokenized, "04 Apr 2026.pdf")
            assertTrue(result.isSuccess)
            val (payslip, report) = result.getOrNull()!!

            assertNotNull(payslip)
            assertEquals(GrammarFamily.PCDA_EXTENDED_GRID, report.selectedFamily)
            assertTrue(report.isKnownGrammar)
            assertEquals(158870.0, payslip.summary.grossPay)
        }
}
