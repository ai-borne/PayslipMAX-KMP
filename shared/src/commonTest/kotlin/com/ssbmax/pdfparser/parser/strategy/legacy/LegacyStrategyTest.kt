package com.ssbmax.pdfparser.parser.strategy.legacy

import com.ssbmax.pdfparser.parser.GrammarAwareParser
import com.ssbmax.pdfparser.parser.PositionedToken
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.detection.GrammarFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LegacyStrategyTest {
    @Test
    fun testLegacyStatementStrategyExecution() {
        val fullText =
            """
            01/2014 STATEMENT OF ACCOUNT FOR 01/2014
            NAME : OFFICER OFFICER
            Total Credit 50000 Total Debit 0 REMITTANCE 50000
            """.trimIndent()

        val mockTokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = fullText,
            )

        val result = GrammarAwareParser.parseWithDiagnostics(mockTokenized, "01 Jan 2014.pdf")
        assertTrue(result.isSuccess)
        val (payslip, report) = result.getOrNull()!!

        assertNotNull(payslip)
        assertEquals(GrammarFamily.PCDA_LEGACY_STATEMENT, report.selectedFamily)
        assertTrue(report.isKnownGrammar)
        assertEquals("LegacyHeaderStrategy", report.selectedStrategies["HeaderStrategy"])
    }

    @Test
    fun testEarlyDualColStrategyExecution() {
        val tableTokens =
            listOf(
                PositionedToken("Band", 10f, 10f, 30f, 15f),
                PositionedToken("Pay", 45f, 10f, 20f, 15f),
                PositionedToken("45000", 70f, 10f, 40f, 15f),
                PositionedToken("Grade", 10f, 30f, 30f, 15f),
                PositionedToken("Pay", 45f, 30f, 20f, 15f),
                PositionedToken("10000", 70f, 30f, 40f, 15f),
            )
        val fullText =
            """
            05/2016 STATEMENT OF ACCOUNT FOR 05/2016
            CDA A/C NO : 16/000/000000X
            Band Pay 45000 Grade Pay 10000
            Total Credit 55000 Total Debit 0 REMITTANCE 55000
            """.trimIndent()

        val mockTokenized =
            TokenizedPayslip(
                tableTokens = tableTokens,
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = fullText,
            )

        val result = GrammarAwareParser.parseWithDiagnostics(mockTokenized, "05 May 2016.pdf")
        assertTrue(result.isSuccess)
        val (payslip, report) = result.getOrNull()!!

        assertNotNull(payslip)
        assertEquals(GrammarFamily.PCDA_EARLY_DUAL_COL, report.selectedFamily)
        assertTrue(report.isKnownGrammar)
        assertEquals(55000.0, payslip.summary.grossPay)
    }
}
