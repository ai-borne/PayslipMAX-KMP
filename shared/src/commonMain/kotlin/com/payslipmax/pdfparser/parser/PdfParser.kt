package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.ParsedPayslip

interface PdfParser {
    /**
     * Decrypts a password-protected payslip PDF and parses its text content.
     * @param pdfBytes The raw bytes of the PDF file.
     * @param password The decryption password (e.g. "535d04").
     * @return A Result containing the ParsedPayslip or an exception on failure.
     */
    suspend fun decryptAndParse(
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
    suspend fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String = "payslip.pdf",
    ): Result<TokenizedPayslip>
}

expect class PlatformPdfParser() : PdfParser {
    override suspend fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip>

    override suspend fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<TokenizedPayslip>
}
