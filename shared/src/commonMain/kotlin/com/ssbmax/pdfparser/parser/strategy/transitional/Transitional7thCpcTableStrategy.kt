package com.ssbmax.pdfparser.parser.strategy.transitional

import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.strategy.IGrammarTableStrategy

/**
 * Table strategy for 7th CPC era key-value text column layouts.
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object Transitional7thCpcTableStrategy : IGrammarTableStrategy {
    override fun extractRawEarnings(tokenized: TokenizedPayslip): Map<String, Double> = emptyMap()

    override fun extractRawDeductions(tokenized: TokenizedPayslip): Map<String, Double> = emptyMap()
}
