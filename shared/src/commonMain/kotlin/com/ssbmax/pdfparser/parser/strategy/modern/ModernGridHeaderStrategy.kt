package com.ssbmax.pdfparser.parser.strategy.modern

import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.parseOfficer
import com.ssbmax.pdfparser.parser.strategy.IGrammarHeaderStrategy

/**
 * Header strategy for Modern Spatial Grid era (Nov 2023 onwards) metadata.
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object ModernGridHeaderStrategy : IGrammarHeaderStrategy {
    override fun extractOfficer(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): Officer {
        return parseOfficer(cleanedText, 4, 2026)
    }
}
