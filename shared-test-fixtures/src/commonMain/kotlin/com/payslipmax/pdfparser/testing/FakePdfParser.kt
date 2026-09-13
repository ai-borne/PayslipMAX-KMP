package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.parser.PdfParser
import com.payslipmax.pdfparser.parser.TokenizedPayslip

class FakePdfParser : PdfParser {
    var result: Result<ParsedPayslip> = Result.failure(Exception("Not configured"))
    var tokensResult: Result<TokenizedPayslip> = Result.failure(Exception("Not configured"))

    /** Optional per-filename override of [result], for tests needing different outcomes per call. */
    var resultsByFilename: Map<String, Result<ParsedPayslip>> = emptyMap()

    override suspend fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip> {
        return resultsByFilename[filename] ?: result
    }

    override suspend fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<TokenizedPayslip> {
        return tokensResult
    }
}
