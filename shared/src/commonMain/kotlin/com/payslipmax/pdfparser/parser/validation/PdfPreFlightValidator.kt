package com.payslipmax.pdfparser.parser.validation

import com.payslipmax.pdfparser.parser.TokenizedPayslip

sealed class PdfPreFlightResult {
    data object Valid : PdfPreFlightResult()

    data class Invalid(val reason: String) : PdfPreFlightResult()
}

object PdfPreFlightValidator {
    private val statementHeaderSignatures =
        listOf(
            "STATEMENT OF ACCOUNT",
            "STATEMENT OF PAY",
            "PAY SLIP FOR",
            "PAY AND ALLOWANCES FOR",
            "PAYSLIP FOR",
            "PAY SLIP OF",
            "PCDA(O) PUNE STATEMENT",
            "PCDA (O) PUNE STATEMENT",
            "JOINING REPORT",
        )

    private val totalsSignatures =
        listOf(
            "GROSS PAY",
            "TOTAL DEDUCTIONS",
            "NET REMITTANCE",
            "TOTAL CREDITS",
            "TOTAL DEBITS",
            "NET PAY",
        )

    fun validate(tokenized: TokenizedPayslip): PdfPreFlightResult {
        val fullText = tokenized.fullText.trim()
        if (fullText.isEmpty() && tokenized.tableTokens.isEmpty()) {
            return PdfPreFlightResult.Invalid("NO_TEXT_TOKENS")
        }

        val upperText = fullText.uppercase()
        if (upperText.contains("PASSWORD PROTECTED") || upperText.contains("[ENCRYPTED]")) {
            return PdfPreFlightResult.Invalid("PASSWORD_PROTECTED")
        }

        // Must contain at least one statement header signature or printed totals signature
        val hasHeaderSignature = statementHeaderSignatures.any { signature -> upperText.contains(signature) }
        val hasTotalsSignature = totalsSignatures.any { signature -> upperText.contains(signature) }

        if (!hasHeaderSignature && !hasTotalsSignature) {
            return PdfPreFlightResult.Invalid("UNRECOGNIZED_GRAMMAR")
        }

        return PdfPreFlightResult.Valid
    }
}
