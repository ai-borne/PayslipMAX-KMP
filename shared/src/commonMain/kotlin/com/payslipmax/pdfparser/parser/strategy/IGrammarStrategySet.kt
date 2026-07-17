package com.payslipmax.pdfparser.parser.strategy

/**
 * Strategy bundle aggregating header, table, and page strategies for a document family.
 * Must be 100% stateless and side-effect free.
 */
interface IGrammarStrategySet {
    val headerStrategy: IGrammarHeaderStrategy
    val pageStrategy: IGrammarPageStrategy
}
