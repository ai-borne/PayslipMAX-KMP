package com.ssbmax.pdfparser.parser.strategy

import com.ssbmax.pdfparser.parser.TokenizedPayslip

/**
 * Strategy interface for grammar-specific table geometry and column token extraction.
 * Must be 100% stateless and side-effect free.
 */
interface IGrammarTableStrategy {
    fun extractRawEarnings(tokenized: TokenizedPayslip): Map<String, Double>

    fun extractRawDeductions(tokenized: TokenizedPayslip): Map<String, Double>
}
