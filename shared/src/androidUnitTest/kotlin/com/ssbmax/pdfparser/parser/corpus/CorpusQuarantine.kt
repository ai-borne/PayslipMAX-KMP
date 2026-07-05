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
 *
 * Jul 2020 - Dec 2021 (19 ids, backfilled corpus capture) shows the identical genuine DSOP-page content
 * divergence pattern (600-740 combined token diff per id) despite sitting inside the same
 * `PCDA_TRANSITIONAL_7TH_CPC` grammar family as the non-divergent 2018-2019/2015-2017 fixtures — the
 * DSOP page's print template apparently changed mid-era, independent of the main statement grammar.
 * Re-verified the same way: 0/19 structured-field mismatches vs ground truth, so quarantining costs no
 * real parity proof here either.
 *
 * [knownEngineGaps] is a separate, smaller list: ids with a documented *production-engine* gap (not a
 * platform divergence) — the same ids and reasons as `TokenParseCorpusRegressionTest`'s `quarantine`
 * set. Their structured-field mismatch on the iOS token path is the identical already-accepted gap
 * seen on the Android path, not new signal.
 */
object CorpusQuarantine {
    val legacyDsopQuarantine =
        listOf(
            Regex(".*_2022$"),
            Regex("0[1-9]_.*_2023$"),
            Regex("10_oct_2023$"),
            Regex(".*_2020(_print)?$"),
            Regex(".*_2021(_print)?$"),
            Regex(".*_20(_print)?$"),
            Regex(".*_21(_print)?$"),
        )

    private val knownEngineGaps =
        setOf(
            // Real arrears-recap-table credit the transaction-table classifier doesn't parse (§
            // TokenParseCorpusRegressionTest for the full reason).
            "10_oct_2019",
            // Complex multi-page pay-fixation/arrears document; ground truth pending manual
            // verification against the original PDF (§ TokenParseCorpusRegressionTest).
            "05_may_2017",
        )

    fun isQuarantined(id: String): Boolean = legacyDsopQuarantine.any { it.matches(id) } || id in knownEngineGaps
}
