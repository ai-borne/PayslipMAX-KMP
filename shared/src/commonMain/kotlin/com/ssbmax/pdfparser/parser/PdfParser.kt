package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip

interface PdfParser {
    /**
     * Decrypts a password-protected payslip PDF and parses its text content.
     * @param pdfBytes The raw bytes of the PDF file.
     * @param password The decryption password (e.g. "535d04").
     * @return A Result containing the ParsedPayslip or an exception on failure.
     */
    fun decryptAndParse(pdfBytes: ByteArray, password: String): Result<ParsedPayslip>
}

expect class PlatformPdfParser() : PdfParser {
    override fun decryptAndParse(pdfBytes: ByteArray, password: String): Result<ParsedPayslip>
}
