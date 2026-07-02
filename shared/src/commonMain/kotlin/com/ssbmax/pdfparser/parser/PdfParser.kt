package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip

interface PdfParser {
    /**
     * Decrypts a password-protected payslip PDF and parses its text content.
     * @param pdfBytes The raw bytes of the PDF file.
     * @param password The decryption password (e.g. "535d04").
     * @return A Result containing the ParsedPayslip or an exception on failure.
     */
    fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String = "payslip.pdf",
    ): Result<ParsedPayslip>

    /**
     * Decrypts a password-protected payslip PDF and emits the platform-independent token IR
     * (Phase 2). Both platforms produce an identical [TokenizedPayslip] so all downstream grid
     * reconstruction and reconciliation lives in common, device-independent code.
     * @return A Result containing the [TokenizedPayslip] or an exception on failure.
     */
    fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String = "payslip.pdf",
    ): Result<TokenizedPayslip>
}

expect class PlatformPdfParser() : PdfParser {
    override fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip>

    override fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<TokenizedPayslip>
}
