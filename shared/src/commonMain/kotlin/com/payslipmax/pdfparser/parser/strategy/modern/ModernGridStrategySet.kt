package com.payslipmax.pdfparser.parser.strategy.modern

import com.payslipmax.pdfparser.parser.strategy.IGrammarHeaderStrategy
import com.payslipmax.pdfparser.parser.strategy.IGrammarPageStrategy
import com.payslipmax.pdfparser.parser.strategy.IGrammarStrategySet

/**
 * Strategy bundle for the Modern Spatial Grid era (Nov 2023–Feb 2025).
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object ModernGridStrategySet : IGrammarStrategySet {
    override val headerStrategy: IGrammarHeaderStrategy = ModernGridHeaderStrategy
    override val pageStrategy: IGrammarPageStrategy = ModernGridPageStrategy
}

/**
 * Strategy bundle for the Extended Multi-Container Spatial Grid era (Mar 2025 onwards).
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object ExtendedGridStrategySet : IGrammarStrategySet {
    override val headerStrategy: IGrammarHeaderStrategy = ModernGridHeaderStrategy
    override val pageStrategy: IGrammarPageStrategy = ModernGridPageStrategy
}
