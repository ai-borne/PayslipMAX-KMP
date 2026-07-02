package com.ssbmax.pdfparser.parser.strategy.transitional

import com.ssbmax.pdfparser.parser.GrammarAwareParser
import com.ssbmax.pdfparser.parser.PositionedToken
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.detection.GrammarFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Transitional7thCpcStrategyTest {
    @Test
    fun testTransitional7thCpcStrategyExecution() {
        val tableTokens =
            listOf(
                PositionedToken("Basic", 10f, 10f, 30f, 15f),
                PositionedToken("Pay", 45f, 10f, 20f, 15f),
                PositionedToken("132400", 70f, 10f, 40f, 15f),
                PositionedToken("DA", 10f, 30f, 20f, 15f),
                PositionedToken("45849", 70f, 30f, 40f, 15f),
            )
        val fullText =
            """
            01/2022 STATEMENT OF ACCOUNT FOR 01/2022
            CDA A/C NO : 16/000/000000X
            NAME  : OFFICER OFFICER OFFICER
            Basic Pay 132400
            DA 45849
            MSP 15500
            Total Credit 193749 Total Debit 0
            REMITTANCE 193749
            """.trimIndent()

        val mockTokenized =
            TokenizedPayslip(
                tableTokens = tableTokens,
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = fullText,
            )

        val result = GrammarAwareParser.parseWithDiagnostics(mockTokenized, "01 Jan 2022.pdf")
        assertTrue(result.isSuccess)
        val (payslip, report) = result.getOrNull()!!

        assertNotNull(payslip)
        assertEquals(GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC, report.selectedFamily)
        assertTrue(report.isKnownGrammar)
        assertEquals("Transitional7thCpcHeaderStrategy", report.selectedStrategies["HeaderStrategy"])
        assertEquals(16, payslip.officer.accountNo.take(2).toInt())
    }
}
