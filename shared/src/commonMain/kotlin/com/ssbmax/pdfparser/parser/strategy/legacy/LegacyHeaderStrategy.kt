package com.ssbmax.pdfparser.parser.strategy.legacy

import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.parseOfficer
import com.ssbmax.pdfparser.parser.strategy.IGrammarHeaderStrategy

/**
 * Header strategy for historical legacy statement formats (2014–2017).
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object LegacyHeaderStrategy : IGrammarHeaderStrategy {
    override fun extractOfficer(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): Officer {
        return parseOfficer(cleanedText, 1, 2014)
    }
}
