package com.payslipmax.pdfparser.parser.strategy

import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.parser.TokenizedPayslip

/**
 * Strategy interface for grammar-specific header and officer metadata extraction.
 * Must be 100% stateless and side-effect free.
 */
interface IGrammarHeaderStrategy {
    fun extractOfficer(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): Officer
}
