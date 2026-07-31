package com.payslipmax.pdfparser.parser.validation

import com.payslipmax.pdfparser.parser.PositionedToken
import com.payslipmax.pdfparser.parser.TokenizedPayslip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfPreFlightValidatorTest {
    @Test
    fun testPasswordProtectedPdf_returnsEncryptedStatus() {
        val tokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "This PDF document is password protected.",
            )
        val result = PdfPreFlightValidator.validate(tokenized)
        assertTrue(result is PdfPreFlightResult.Invalid)
        assertEquals("PASSWORD_PROTECTED", (result as PdfPreFlightResult.Invalid).reason)
    }

    @Test
    fun testZeroTokenPdf_returnsInsufficientTokensStatus() {
        val tokenized =
            TokenizedPayslip(
                tableTokens = emptyList(),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "   ",
            )
        val result = PdfPreFlightValidator.validate(tokenized)
        assertTrue(result is PdfPreFlightResult.Invalid)
        assertEquals("NO_TEXT_TOKENS", (result as PdfPreFlightResult.Invalid).reason)
    }

    @Test
    fun testValidTokenizedPdf_returnsValidStatus() {
        val tokenized =
            TokenizedPayslip(
                tableTokens = listOf(PositionedToken("BASIC", 10f, 10f, 50f, 12f, 10f)),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "PCDA (O) PUNE STATEMENT OF ACCOUNT PAY AND ALLOWANCES FOR MAR 2025 BASIC PAY",
            )
        val result = PdfPreFlightValidator.validate(tokenized)
        assertEquals(PdfPreFlightResult.Valid, result)
    }

    @Test
    fun testPcdaAbbreviationGuidePdf_returnsUnrecognizedFormatStatus() {
        val tokenized =
            TokenizedPayslip(
                tableTokens = listOf(PositionedToken("DSOP", 10f, 10f, 50f, 12f, 10f)),
                taxTokens = emptyList(),
                dsopTokens = emptyList(),
                fullText = "Welcome to PCDA(O) Pune https://pcdaopune.gov.in/user/abbreviation Page 3 of 15 Dearness Allowance DSOP INTEREST",
            )
        val result = PdfPreFlightValidator.validate(tokenized)
        assertTrue(result is PdfPreFlightResult.Invalid)
        assertEquals("UNRECOGNIZED_GRAMMAR", (result as PdfPreFlightResult.Invalid).reason)
    }
}
