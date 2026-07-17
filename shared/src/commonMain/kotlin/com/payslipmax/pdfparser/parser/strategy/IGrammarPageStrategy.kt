package com.payslipmax.pdfparser.parser.strategy

import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.parser.TokenizedPayslip

/**
 * Strategy interface for grammar-specific multi-page document structure (tax, DSOP, etc.).
 * Must be 100% stateless and side-effect free.
 */
interface IGrammarPageStrategy {
    fun extractTaxAndSavings(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): TaxAndSavings?
}
