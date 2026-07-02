package com.ssbmax.pdfparser.parser.strategy.transitional

import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.parseOfficer
import com.ssbmax.pdfparser.parser.strategy.IGrammarHeaderStrategy

/**
 * Header strategy for 7th CPC era (2018–Oct 2023) payslip metadata.
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object Transitional7thCpcHeaderStrategy : IGrammarHeaderStrategy {
    override fun extractOfficer(
        tokenized: TokenizedPayslip,
        cleanedText: String,
    ): Officer {
        return parseOfficer(cleanedText, 1, 2022)
    }
}
