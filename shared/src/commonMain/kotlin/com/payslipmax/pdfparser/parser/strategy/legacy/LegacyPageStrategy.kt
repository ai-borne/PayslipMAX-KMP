package com.payslipmax.pdfparser.parser.strategy.legacy

import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.parser.TokenText
import com.payslipmax.pdfparser.parser.TokenizedPayslip
import com.payslipmax.pdfparser.parser.parseTaxAndSavings
import com.payslipmax.pdfparser.parser.strategy.IGrammarPageStrategy

/**
 * Page strategy for historical legacy statement formats.
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object LegacyPageStrategy : IGrammarPageStrategy {
    override fun extractTaxAndSavings(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): TaxAndSavings? {
        val taxText = TokenText.readingOrder(tokenized.taxTokens)
        val dsopText = TokenText.readingOrder(tokenized.dsopTokens)
        return parseTaxAndSavings(taxText, dsopText, cleanedText)
    }
}
