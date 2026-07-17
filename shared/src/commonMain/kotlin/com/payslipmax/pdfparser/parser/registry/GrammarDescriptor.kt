package com.payslipmax.pdfparser.parser.registry

import com.payslipmax.pdfparser.parser.TokenizedPayslip
import com.payslipmax.pdfparser.parser.detection.GrammarFamily
import com.payslipmax.pdfparser.parser.detection.GrammarMatchResult
import com.payslipmax.pdfparser.parser.strategy.IGrammarStrategySet

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
    /**
     * Broad sanity check run only when the statement period already selected this family
     * (see [com.payslipmax.pdfparser.parser.detection.GrammarEraMapper]): confirms the document plausibly
     * belongs to this family at all, without trying to distinguish it from a neighboring era the way
     * [detectorMatcher] must when no date is available. Defaults to [detectorMatcher] for families
     * whose detector is already broad enough to double as verification.
     */
    val verificationMatcher: (TokenizedPayslip) -> GrammarMatchResult = detectorMatcher,
)
