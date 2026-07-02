package com.ssbmax.pdfparser.parser.strategy.transitional

import com.ssbmax.pdfparser.parser.strategy.IGrammarHeaderStrategy
import com.ssbmax.pdfparser.parser.strategy.IGrammarPageStrategy
import com.ssbmax.pdfparser.parser.strategy.IGrammarStrategySet

/**
 * Strategy bundle for the 7th CPC Transitional era (2018–Oct 2023).
 * Immutable, 100% stateless and side-effect free (<50 lines).
 */
object Transitional7thCpcStrategySet : IGrammarStrategySet {
    override val headerStrategy: IGrammarHeaderStrategy = Transitional7thCpcHeaderStrategy
    override val pageStrategy: IGrammarPageStrategy = Transitional7thCpcPageStrategy
}
