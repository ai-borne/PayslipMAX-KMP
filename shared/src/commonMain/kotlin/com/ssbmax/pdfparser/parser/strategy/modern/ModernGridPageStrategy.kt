package com.ssbmax.pdfparser.parser.strategy.modern

import com.ssbmax.pdfparser.domain.TaxAndSavings
import com.ssbmax.pdfparser.parser.TokenText
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.parseTaxAndSavings
import com.ssbmax.pdfparser.parser.strategy.IGrammarPageStrategy

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
