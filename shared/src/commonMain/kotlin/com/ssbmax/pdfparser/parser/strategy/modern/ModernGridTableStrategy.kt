package com.ssbmax.pdfparser.parser.strategy.modern

import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.strategy.IGrammarTableStrategy

/**
 * Table strategy for Modern Spatial Grid multi-container layouts.
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object ModernGridTableStrategy : IGrammarTableStrategy {
    override fun extractRawEarnings(tokenized: TokenizedPayslip): Map<String, Double> = emptyMap()

    override fun extractRawDeductions(tokenized: TokenizedPayslip): Map<String, Double> = emptyMap()
}
