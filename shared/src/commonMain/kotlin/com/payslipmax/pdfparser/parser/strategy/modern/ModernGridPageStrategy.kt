package com.payslipmax.pdfparser.parser.strategy.modern

import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.parser.TokenText
import com.payslipmax.pdfparser.parser.TokenizedPayslip
import com.payslipmax.pdfparser.parser.parseTaxAndSavings
import com.payslipmax.pdfparser.parser.strategy.IGrammarPageStrategy

/**
 * Page strategy for Modern Spatial Grid multi-page structure (tax, DSOP, DO2, Contact pages).
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object ModernGridPageStrategy : IGrammarPageStrategy {
    override fun extractTaxAndSavings(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): TaxAndSavings? {
        val taxText = TokenText.readingOrder(tokenized.taxTokens)
        val dsopText = TokenText.readingOrder(tokenized.dsopTokens)
        return parseTaxAndSavings(taxText, dsopText, cleanedText)
    }
}
