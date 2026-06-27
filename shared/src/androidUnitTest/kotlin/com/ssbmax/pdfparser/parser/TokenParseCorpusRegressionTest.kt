package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.parser.corpus.CorpusFixtures
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Phase 4 cut-over gate. It drives the **new primary** [PayslipTokenParser] over the committed,
 * de-identified token fixtures (geometry from `<id>.tokens.json`, document text from `<id>.input.json`)
 * and diffs the full [com.ssbmax.pdfparser.domain.ParsedPayslip] against human-verified ground truth.
 *
 * This is the structural proof that the token engine *meets or beats* the Phase 0 string-path baseline:
 * it passes all 52/52 fixtures with an empty quarantine. It is fully offline — no device, no PDFs, no PII.
 *
 * ### Ground-truth correction (Phase 4)
 * Four months (Apr-2022, Mar/Apr/Jun-2023) previously relied on the legacy
 * DynamicSpatialParser.applyHistoricalOverrides (removed in Phase 4) micro-fudge, which sprinkled a small unattributed
 * residual (₹14–₹79) onto a named earning (Basic Pay / DA / MSP / Ration / SF) to force a balance. That
 * made those fixtures internally inconsistent — their earnings summed *past* the ledger-adjusted gross by
 * exactly the fudge. Phase 4 deletes the override and corrects those four fixtures to the **real on-page
 * values** (verified against the committed tokens), after which the engine reconciles each to its printed
 * gross with `miscEarnings == 0`. The [quarantine] mechanism is retained (empty) so any future known-bad
 * month can be parked with a documented reason without breaking the build.
 */
class TokenParseCorpusRegressionTest {
    /** Fixture ids the token engine is known to get wrong, each with a documented reason. Empty at cut-over. */
    private val quarantine: Set<String> = emptySet()

    @Test
    fun tokenEngineMeetsOrBeatsBaseline() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — fixtures missing from resources/corpus/")

        val regressions = mutableListOf<String>()
        val staleQuarantine = mutableListOf<String>()

        for (id in ids) {
            val tokens = CorpusFixtures.loadTokens(id)
            val input = CorpusFixtures.loadInput(id)
            val tokenized =
                TokenizedPayslip(
                    tableTokens = tokens.tableTokens,
                    taxTokens = tokens.taxTokens,
                    dsopTokens = tokens.dsopTokens,
                    fullText = input.fullText,
                )

            val result = PayslipTokenParser.parse(tokenized, input.filename)
            val parsed = result.getOrNull()
            val expected = CorpusFixtures.loadExpected(id)
            val mismatches =
                if (parsed == null) {
                    listOf("parse failed: ${result.exceptionOrNull()?.message}")
                } else {
                    CorpusFixtures.diff(expected, parsed)
                }

            val isQuarantined = id in quarantine
            when {
                mismatches.isEmpty() && isQuarantined ->
                    staleQuarantine += "$id now PASSES — remove it from quarantine (Phase 4 residual resolved)."
                mismatches.isNotEmpty() && !isQuarantined ->
                    regressions += "$id: ${mismatches.size} mismatch(es) -> ${mismatches.take(6)}"
            }
        }

        val problems = regressions + staleQuarantine
        assertTrue(
            problems.isEmpty(),
            "Token-path corpus regression (${ids.size} fixtures, ${quarantine.size} quarantined):\n" +
                problems.joinToString("\n"),
        )
    }
}
