package com.ssbmax.pdfparser.parser.strategy

import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.parser.TokenizedPayslip

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
