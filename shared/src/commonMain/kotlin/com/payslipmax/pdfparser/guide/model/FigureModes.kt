package com.payslipmax.pdfparser.guide.model

/** The `mode` strings of a [GuideFigure], spelt once so the resolver, the validator and the pre-claim calculators cannot drift apart. */
internal object FigureModes {
    const val DA_STEP = "da_step"
    const val PERCENT_OF_BASIC = "percent_of_basic"
    const val PLUS_DA = "plus_da"
    const val RATE_TABLE = "rate_table"
    const val RATE_BY_STATE = "rate_by_state"
}
