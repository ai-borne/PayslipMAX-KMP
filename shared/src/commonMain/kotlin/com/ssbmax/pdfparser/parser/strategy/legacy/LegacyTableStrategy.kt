package com.ssbmax.pdfparser.parser.strategy.legacy

import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.strategy.IGrammarTableStrategy

/**
 * Table strategy for historical legacy statement flat key-value text streams.
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object LegacyTableStrategy : IGrammarTableStrategy {
    override fun extractRawEarnings(tokenized: TokenizedPayslip): Map<String, Double> = emptyMap()

    override fun extractRawDeductions(tokenized: TokenizedPayslip): Map<String, Double> = emptyMap()
}
