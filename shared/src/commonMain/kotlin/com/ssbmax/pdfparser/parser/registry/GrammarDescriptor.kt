package com.ssbmax.pdfparser.parser.registry

import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.detection.GrammarFamily
import com.ssbmax.pdfparser.parser.detection.GrammarMatchResult
import com.ssbmax.pdfparser.parser.strategy.IGrammarStrategySet

/**
 * Self-contained descriptor encapsulating document grammar identification, deterministic rules,
 * and strategy bindings for a PCDA(O) document family.
 *
 * Immutable, 100% stateless and side-effect free.
 */
data class GrammarDescriptor(
    val family: GrammarFamily,
    val priority: Int,
    val displayName: String,
    val detectorMatcher: (TokenizedPayslip) -> GrammarMatchResult,
    val strategySet: IGrammarStrategySet,
)
