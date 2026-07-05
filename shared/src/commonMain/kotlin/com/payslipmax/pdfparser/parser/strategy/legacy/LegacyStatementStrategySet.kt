package com.payslipmax.pdfparser.parser.strategy.legacy

import com.payslipmax.pdfparser.parser.strategy.IGrammarHeaderStrategy
import com.payslipmax.pdfparser.parser.strategy.IGrammarPageStrategy
import com.payslipmax.pdfparser.parser.strategy.IGrammarStrategySet

/**
 * Strategy bundle for the 2014 Legacy Statement era.
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object LegacyStatementStrategySet : IGrammarStrategySet {
    override val headerStrategy: IGrammarHeaderStrategy = LegacyHeaderStrategy
    override val pageStrategy: IGrammarPageStrategy = LegacyPageStrategy
}

/**
 * Strategy bundle for the Early Dual-Column era (2015–2017).
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object EarlyDualColStrategySet : IGrammarStrategySet {
    override val headerStrategy: IGrammarHeaderStrategy = LegacyHeaderStrategy
    override val pageStrategy: IGrammarPageStrategy = LegacyPageStrategy
}
