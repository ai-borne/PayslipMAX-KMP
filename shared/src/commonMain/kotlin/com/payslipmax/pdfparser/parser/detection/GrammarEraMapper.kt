package com.payslipmax.pdfparser.parser.detection

/**
 * Maps a [StatementPeriod] to the [GrammarFamily] whose document layout was in effect that month.
 * This is the single source of truth for era boundaries — see [GrammarFamily] for the layout
 * description of each family. The top boundary is intentionally open-ended: any period at or after
 * [EXTENDED_GRID_START] resolves to [GrammarFamily.PCDA_EXTENDED_GRID], so future months need no
 * code change until a genuinely new document layout ships.
 */
object GrammarEraMapper {
    // Boundaries are the first month each family's layout was observed in the field.
    private val EARLY_DUAL_COL_START = StatementPeriod(month = 1, year = 2015).ordinal
    private val TRANSITIONAL_7TH_CPC_START = StatementPeriod(month = 1, year = 2018).ordinal
    private val MODERN_GRID_START = StatementPeriod(month = 11, year = 2023).ordinal
    private val EXTENDED_GRID_START = StatementPeriod(month = 3, year = 2025).ordinal

    fun mapToFamily(period: StatementPeriod): GrammarFamily {
        val ordinal = period.ordinal
        return when {
            ordinal < EARLY_DUAL_COL_START -> GrammarFamily.PCDA_LEGACY_STATEMENT
            ordinal < TRANSITIONAL_7TH_CPC_START -> GrammarFamily.PCDA_EARLY_DUAL_COL
            ordinal < MODERN_GRID_START -> GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC
            ordinal < EXTENDED_GRID_START -> GrammarFamily.PCDA_MODERN_GRID
            else -> GrammarFamily.PCDA_EXTENDED_GRID
        }
    }

    /** Human-readable era label for diagnostics, e.g. "Mar 2025+". */
    fun eraLabel(family: GrammarFamily): String =
        when (family) {
            GrammarFamily.PCDA_LEGACY_STATEMENT -> "pre Jan 2015"
            GrammarFamily.PCDA_EARLY_DUAL_COL -> "Jan 2015 - Dec 2017"
            GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC -> "Jan 2018 - Oct 2023"
            GrammarFamily.PCDA_MODERN_GRID -> "Nov 2023 - Feb 2025"
            GrammarFamily.PCDA_EXTENDED_GRID -> "Mar 2025+"
            GrammarFamily.UNKNOWN -> "unknown"
        }
}
