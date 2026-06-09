package com.ssbmax.pdfparser.testing

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.parser.PdfParser

class FakePdfParser : PdfParser {
    var result: Result<ParsedPayslip> = Result.failure(Exception("Not configured"))

    override fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
    ): Result<ParsedPayslip> {
        return result
    }
}
