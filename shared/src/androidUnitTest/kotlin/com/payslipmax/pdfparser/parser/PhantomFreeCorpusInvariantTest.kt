package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Phase 0 of the phantom-numbers sprint (see `docs/AI_INSIGHTS_PIPELINE.md` changelog) — the
 * assertion surface for the ledger-phantom invariants, run against the **production engine**
 * [GrammarAwareParser] over every committed corpus fixture:
 *
 * - **D1**: no raw earnings/deductions entry is phantom-shaped (bare statement-title/increment-date
 *   year, pin-code-shaped 6-digit value, or a label with no alphabetic content) —
 *   [CorpusFixtures.phantomRawEntries].
 * - **D3**: every fixture reconciles within [com.payslipmax.pdfparser.parser.SchemaValidator]'s
 *   tolerance, or is already flagged `needsReview` — [CorpusFixtures.reconcilesOrFlagged].
 *
 * This is enabling infrastructure only (no engine change in this phase). Fixtures the engine
 * currently fails are parked in [quarantine] with a documented reason, mirroring
 * [TokenParseCorpusRegressionTest]'s pattern, so this test stays green until Phase 1-3 fix the
 * underlying geometry/reconciliation gaps and empty the quarantine (Phase 4).
 *
 * Runs over [CorpusFixtures.loadIndex] plus [extraIds]: `apr_14` (the 2014 full-address-block
 * fixture captured for this sprint, see the plan's Phase 0) is deliberately kept out of the shared
 * `index.json` — that index also feeds [IosTokenParseCorpusRegressionTest], which hard-requires a
 * committed iOS PDFKit token dump for every id with no missing-data escape hatch, and no iOS capture
 * device/simulator was available to produce one for this new fixture. It is exercised here directly
 * (currently passing D1/D3 with today's engine) and should be folded into the shared index once its
 * iOS tokens are captured.
 */
class PhantomFreeCorpusInvariantTest {
    private val extraIds = listOf("apr_14")

    /**
     * Fixture ids with a known, documented phantom/reconciliation gap. Emptied by Phase 4.
     *
     * Verified by inspection (Phase 0): the sprint's hypothesized header/footer pin-year leak
     * (`apr_14`'s address block, the Dec-fixtures' seasonal-greeting year) does *not* currently reach
     * the raw channel — `TokenTableClassifier`'s column-band acceptance radius already drops those
     * tokens as "outside acceptance radius". The gaps that *do* reproduce today are a different,
     * still-in-scope instance of the same root cause: the "Details of Transactions" narrative text
     * (quarter-allotment notes carrying an office PIN code) landing inside the credit/debit column
     * band and leaking into `rawEarnings`/`rawDeductions`.
     *
     * Phase 1's [VerticalBandFilter] closed 9 of the original 10 entries: the office-PIN leaks
     * (`04_apr_2021`, `07_jul_2020`, `08_aug_2020`, `09_sep_2020`, `10_oct_20`, `04_apr_2023`,
     * `05_may_2023`, `06_jun_2023`, `07_jul_2023`) all turned out to sit *below* the printed totals
     * row — a "Details of Transactions during the month" note printed under the main table, not
     * interspersed between real line items — so the learned bottom bound now drops them directly.
     *
     * Only `01_jan_18` remains, and it turned out **not** to be Phase 2's scope after all: its two
     * raw deductions (`'R/oEtkt'=1915`, `'TA Debit'=22314`, aggregated from two narrative sub-notes)
     * sit *inside* the vertical table body with real alphabetic labels — neither "no alphabetic
     * content" nor "solely date/place stopwords" (Phase 2's [RawLabelNoiseFilter.isDatePlaceOnlyNoise]
     * predicate, confirmed against this exact case before implementation) fires on them, so a
     * label-shape rule cannot safely remove them without risking a real allowance whose label happens
     * to be terse. What *does* prove them phantom: `1915 + 22314 = 24229`, exactly the fixture's
     * existing reconciliation residual — Phase 3's totals-driven arithmetic removal is the correct,
     * principled fix, not a broadened stopword list.
     */
    private val quarantine: Map<String, String> =
        mapOf(
            "01_jan_18" to
                "D1: 'Details of Transactions' narrative ('R/oEtkt 1915' — recovery-of-e-ticket note) " +
                "leaks a raw deduction whose value coincidentally falls in the bare-year range; a real " +
                "rupee amount, not a statement year — flags the noise-column leak, not a year phantom. " +
                "Sits inside the vertical table body (unlike the other 9, now-fixed entries), so Phase " +
                "1's vertical-band filter cannot distinguish it by geometry alone — Phase 2 scope.",
        )

    @Test
    fun corpusIsPhantomFreeAndReconciles() {
        val ids = CorpusFixtures.loadIndex() + extraIds
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — fixtures missing from resources/corpus/")

        val violations = mutableListOf<String>()
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

            val parsed = GrammarAwareParser.parse(tokenized, input.filename).getOrNull()
            val reasons = mutableListOf<String>()
            if (parsed == null) {
                reasons += "parse failed"
            } else {
                val phantoms = CorpusFixtures.phantomRawEntries(parsed)
                if (phantoms.isNotEmpty()) reasons += "D1 violated: $phantoms"
                if (!CorpusFixtures.reconcilesOrFlagged(parsed)) reasons += "D3 violated: unreconciled and not flagged for review"
            }

            val isQuarantined = id in quarantine
            when {
                reasons.isEmpty() && isQuarantined ->
                    staleQuarantine += "$id now PASSES — remove it from quarantine."
                reasons.isNotEmpty() && !isQuarantined ->
                    violations += "$id: ${reasons.joinToString("; ")}"
            }
        }

        val problems = violations + staleQuarantine
        assertTrue(
            problems.isEmpty(),
            "Phantom-free corpus invariant (${ids.size} fixtures, ${quarantine.size} quarantined):\n" +
                problems.joinToString("\n"),
        )
    }
}
