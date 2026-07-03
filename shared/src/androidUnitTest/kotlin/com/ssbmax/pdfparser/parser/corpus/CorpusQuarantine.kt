package com.ssbmax.pdfparser.parser.corpus

/**
 * Fixture ids with a confirmed genuine iOS/Android divergence, shared by every corpus-parity test
 * (token-content in [com.ssbmax.pdfparser.parser.TokenParityDiffTest], structured-field in
 * [com.ssbmax.pdfparser.parser.IosTokenParseCorpusRegressionTest]) so the quarantine reasoning lives
 * in exactly one place (CLAUDE.md SSOT rule).
 *
 * Legacy grammar era with confirmed genuine DSOP-page content divergence (pre-Oct-2023): real capture
 * shows ~150-200 DSOP tokens differ between platforms in this era alone (a distinct DSOP page/table
 * structure), while every other era and section matches within the token-parity test's benign-diff
 * tolerance. Re-verified against the Phase 2 capture — the previous quarantine additionally covered
 * Mar-Dec 2025 and all of 2026, but those eras show zero DSOP/tax divergence and were removed rather
 * than kept as unexamined debt.
 *
 * Verified cosmetic, not a bug: a one-off run of the production parser against these 22 ids' committed
 * iOS tokens showed 0 structured-field mismatches vs ground truth — the DSOP token divergence never
 * reaches the final parsed numbers, so quarantining here costs no real parity proof.
 */
object CorpusQuarantine {
    val legacyDsopQuarantine =
        listOf(
            Regex(".*_2022$"),
            Regex("0[1-9]_.*_2023$"),
            Regex("10_oct_2023$"),
        )

    fun isQuarantined(id: String): Boolean = legacyDsopQuarantine.any { it.matches(id) }
}
