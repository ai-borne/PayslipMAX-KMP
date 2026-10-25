package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.model.Part2Lookup

/**
 * The invented Part II lookup with its claim moved out of the first built ribbon into a second one, so a test can tell codes of the same
 * ribbon from codes of another (a cancel slip offers cancel codes only, a leave slip never does). Invented codes only.
 */
object SyntheticPart2Ribbons {
    /** [SyntheticPart2Templates.CLAIM] lives alone in the second ribbon; every other drafted code stays in the first. */
    val twoRibbons: Part2Lookup by lazy {
        val base = SyntheticPart2Templates.lookup
        val first = base.ribbons.first()
        base.copy(
            ribbons =
                base.ribbons.map { r -> r.copy(cases = r.cases.map { c -> if (c.id == "leave_two") c.copy(serials = c.serials - "2.1") else c }) } +
                    first.copy(id = "second", title = "The second ribbon", cases = listOf(first.cases.first().copy(id = "second_case", serials = listOf("2.1")))),
        )
    }
}
