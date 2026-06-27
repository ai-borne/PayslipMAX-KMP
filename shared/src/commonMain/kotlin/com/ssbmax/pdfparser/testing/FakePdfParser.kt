package com.ssbmax.pdfparser.testing

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.parser.PdfParser
import com.ssbmax.pdfparser.parser.TokenizedPayslip

class FakePdfParser : PdfParser {
    var result: Result<ParsedPayslip> = Result.failure(Exception("Not configured"))
    var tokensResult: Result<TokenizedPayslip> = Result.failure(Exception("Not configured"))

    override fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip> {
        return result
    }

    override fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<TokenizedPayslip> {
        return tokensResult
    }
}
