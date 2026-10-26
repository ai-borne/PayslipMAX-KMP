package com.payslipmax.pdfparser.part2.domain

/**
 * The rule family for the final encashment of annual leave on retirement (Part II plan, phase 8; ODP-2026 pp.140-141): the days to encash
 * and the year-wise break-up (note 2) worked out from the typed list, and the two limits the manual states. The break-up is a `years` slot,
 * [YearlyBreakup]'s text; the cap is [AcclveCalculator]'s, so 300 has one home.
 */
object EncashRules {
    const val TOTAL = "encash_total"
    const val LISTING = "encash_listing"
    const val YEAR_OVER_30 = "encash_year_over_30"
    const val OVER_CAP = "encash_over_cap"

    private const val BREAKUP = "breakup"

    /** "2011 30, 2013 30, Total 60": the manual lists the years one under another and ends with their total (p.140); the slip prints the same on one line. */
    private const val TOTAL_WORD = "Total"

    val all: List<Part2Rule> =
        listOf(
            deriveRule(TOTAL) { context -> rows(context.values)?.let { SlotValue.NumberValue(minOf(YearlyBreakup.total(it), AcclveCalculator.TOTAL_CAP_DAYS)) } },
            deriveRule(LISTING) { context -> rows(context.values)?.let { SlotValue.TextValue(YearlyBreakup.format(it).replace("\n", ", ") + ", $TOTAL_WORD ${YearlyBreakup.total(it)}") } },
            checkRule(YEAR_OVER_30) { context -> rows(context.request.values)?.any { it.days > AcclveCalculator.ACCRUAL_CAP_DAYS } ?: false },
            checkRule(OVER_CAP) { context -> (rows(context.request.values)?.let(YearlyBreakup::total) ?: 0) > AcclveCalculator.TOTAL_CAP_DAYS },
        )

    private fun rows(values: Map<String, SlotValue>): List<YearDays>? =
        ((values[BREAKUP] as? SlotValue.TextValue)?.text?.let(YearlyBreakup::parse) as? BreakupRead.Rows)?.rows
}
