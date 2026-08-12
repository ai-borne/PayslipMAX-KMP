package com.payslipmax.pdfparser.tax

import com.payslipmax.pdfparser.domain.TaxRegime
import com.payslipmax.pdfparser.insights.DualRegimeEngine
import com.payslipmax.pdfparser.insights.RegimeTaxOutcome
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator
import com.payslipmax.pdfparser.parser.corpus.CorpusFixtures
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Golden harness (docs/Plan/04_TaxPlannerGoldStandard.md Phase 0 + Phase 1). For every committed corpus
 * fixture with a PCDA tax page, computes tax on PCDA's own `netTaxableIncome` via the production
 * [DualRegimeEngine] and compares to PCDA's own `totalTaxPayable`. Reproduces the §1.3 evidence exactly
 * (see `scripts/analyze_tax_corpus.py`, its Python twin used to derive these exclusion lists).
 *
 * PCDA's printed "Total Tax Payable" is base tax after rebate/marginal relief, WITHOUT cess -- confirmed
 * against the Apr 2026 ground truth (603822 == slab tax on netTaxableIncome 3412740 under Finance Act
 * 2025, no +4% applied). [DualRegimeEngine.calculateOldRegimeTax]/[calculateNewRegimeTax] take *gross*
 * income and subtract the FY's standard deduction internally, so this test backs a synthetic gross out of
 * PCDA's own net-taxable figure (`netTaxableIncome + standardDeduction`) to drive the same production
 * slab/rebate path directly off PCDA's number -- no separate slab math is reimplemented here.
 *
 * Three documented, data-driven exclusion categories (never `@Ignore`) -- a fixture leaving its list
 * silently is a bug in this test, not a reason to delete the check, so [staleExclusions] fails loudly.
 * D3's `CORRUPTED_TOTAL_TAX` category (the sanity-guard bug that fabricated `round(grossSalaryYtd * 0.30)`
 * in place of PCDA's real figure, TaxParserUtils.kt) is deleted entirely in Phase 4: the guard fix plus
 * a corrected re-capture of all 13 affected fixtures against the real PDFs recovered their true PCDA
 * figures, and 12 of the 13 now reproduce exactly with no exclusion needed at all. The 13th
 * (`03_mar_2025`) is folded into `MARCH_SEMANTICS` below rather than force-matched, per its own note.
 */
class PcdaTaxParityTest {
    /** Apr-Nov 2024: PCDA's payroll withheld TDS under stale FY2023-24 slabs until switching in the
     * December 2024 run; the Finance (No.2) Act 2024 slabs govern the whole of FY2024-25 in law, so our
     * engine (correctly) diverges from PCDA's under-withheld TDS in this window. Real, expected
     * divergence -- not a defect (docs/Plan/04_TaxPlannerGoldStandard.md S1.3). */
    private val PCDA_LAG =
        setOf(
            "04_apr_2024",
            "05_may_2024",
            "06_june_2024",
            "07_jul_2024",
            "08_aug_2024",
            "09_sep_2024",
            "10_oct_2024",
            "11_nov_2024",
        )

    /** Phase 4 finding (docs/Plan/04_TaxPlannerGoldStandard.md Open Item 1), split into two distinct facts:
     *
     * 1. `grossSalaryYtd` is confirmed NOT cumulative-YTD in March: across every March fixture in the
     *    corpus, `totalTaxableIncome / grossSalaryYtd` is consistently ~12 (one month's worth), unlike
     *    Jan/Jun/Sep fixtures where the same ratio tracks the calendar month position exactly (e.g. ~1.2
     *    in Jan, month 10 of the FY). This is why the D3 fix ([TaxParserUtils.buildTaxAndSavings]) bounds
     *    `totalTaxPayable` against `totalTaxableIncome`, never `grossSalaryYtd` -- the latter's meaning
     *    changes at FY-end and can't be trusted as a sanity bound in any month.
     * 2. That finding does NOT explain `03_mar_2017` specifically: its `totalTaxPayable` (106568) isn't
     *    guard-corrupted (not `round(grossSalaryYtd * 0.30)`) and doesn't depend on `grossSalaryYtd` at
     *    all here -- this test drives `netTaxableIncome` (970339) straight through the production engine
     *    under the correct FY2016-17 slabs (10% band, verified applied) and still gets 119068, a clean
     *    Rs 12,500 above PCDA's printed figure. Root cause undetermined (a candidate theory: PCDA's
     *    March/year-end tax figure may be carried forward from an earlier provisional computation rather
     *    than recomputed against the final closing `netTaxableIncome` -- unconfirmed). Stays excluded
     *    pending that investigation (Risk register: "fixtures stay excluded until understood -- not
     *    silently included"), not resolved by this phase's D3 fix.
     *
     * `03_march_2022` was provisionally added here too but this test's own staleness check proved it
     * exact (net taxable 44,333 is below the exemption threshold, so tax is trivially 0 regardless of
     * either anomaly above) -- removed per that check.
     *
     * 3. `03_mar_2025` (added during Phase 4's fixture regeneration): its re-captured, no-longer-D3-
     *    corrupted `totalTaxPayable` (535761) is a clean Rs 110,000 BELOW this test's computed figure
     *    (645761, FY2024-25 NEW regime on netTaxableIncome 3185870) -- exactly the flat FY2023-24-vintage
     *    slab overcharge magnitude from D1 (docs/Plan/04_TaxPlannerGoldStandard.md §0/§2), not the D3
     *    fabrication pattern (which this fixture no longer matches either). `PCDA_LAG` above documents the
     *    same stale-slab gap for Apr-Nov 2024; this March 2025 closing statement shows the identical gap,
     *    which the plan's evidence (§1.3: "PCDA applied ... slabs from the December 2024 payroll") did not
     *    anticipate surviving into the FY's final month. Root cause unconfirmed -- possibly PCDA's
     *    March/year-end reconciliation of `Total Tax Payable` reflects cumulative actual TDS withheld
     *    (partly under the old slabs) rather than a full recompute under the corrected law. Excluded here
     *    rather than force-matched, pending investigation. */
    private val MARCH_SEMANTICS = setOf("03_mar_2017", "03_mar_2025")

    /** Not anticipated by the plan: apr_14's `totalTaxableIncome`/`standardDeduction`/`totalTaxPayable`/
     * `cessDeductedYtd` are all 0 despite a populated `netTaxableIncome`, i.e. the tax-page ground truth
     * was never fully captured for this fixture (predates full tax-page field capture). Not a computation
     * defect -- there is no real PCDA figure here to reproduce. Surfaced during Phase 0 evidence-gathering
     * as a 4th category the plan's three lists didn't anticipate.
     *
     * Tech-debt note (found during Phase 4, deliberately left alone): `apr_14` is missing from
     * `resources/corpus/index.json` even though its fixture files are committed, so [CorpusFixtures.loadIndex]
     * never actually surfaces it to this test's main loop -- this exclusion set entry is presently inert.
     * Confirmed why when briefly restoring it to the index: `TokenParseCorpusRegressionTest` fails on it
     * with a Rs 2 rounding mismatch (`summary.totalDeductions`/`netRemittance`, unrelated to tax/D3) that
     * exceeds that test's tolerance. Fixing this needs edits to that test's own quarantine list, which is
     * outside Phase 4's D3 scope -- left excluded from the index, not silently patched over. */
    private val GROUND_TRUTH_GAP = setOf("apr_14")

    private fun detectRegime(id: String): TaxRegime {
        val input = CorpusFixtures.loadInput(id)
        val taxText = input.taxPageText.ifEmpty { input.fullText }
        return if (taxText.contains("New Tax Regime", ignoreCase = true)) TaxRegime.NEW else TaxRegime.OLD
    }

    /** Drives PCDA's own netTaxableIncome through the production engine by backing out a synthetic gross. */
    private fun computedTax(
        netTaxableIncome: Double,
        regime: TaxRegime,
        fy: String,
    ): Double {
        // ADR-2: getRulesForFy is intentionally retained here (the one named legacy caller) purely to
        // read standardDeductionNew/Old for backing out a synthetic gross -- the actual tax computation
        // below still goes through the resolve()-based DualRegimeEngine, so this test carries no
        // silent-wrong-FY risk of its own.
        val rules = TaxRuleKnowledgeBase.getRulesForFy(fy)
        val outcome =
            if (regime == TaxRegime.NEW) {
                val gross = netTaxableIncome + rules.standardDeductionNew
                DualRegimeEngine.calculateNewRegimeTax(gross, fy)
            } else {
                val gross = netTaxableIncome + rules.standardDeductionOld
                DualRegimeEngine.calculateOldRegimeTax(gross, 0.0, fy)
            }
        // Golden harness only ever drives FYs present in the committed corpus, all of which are known
        // to TaxRuleKnowledgeBase -- an OutOfRange result here is a test-data bug, not a case to degrade.
        return when (outcome) {
            is RegimeTaxOutcome.Available -> outcome.detail.baseTax
            is RegimeTaxOutcome.RulesUnavailable ->
                error("PcdaTaxParityTest: FY $fy unexpectedly unresolvable (nearest known: ${outcome.nearestKnownFy})")
        }
    }

    @Test
    fun productionEngineReproducesPcdaAcrossCorpus() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — fixtures missing from resources/corpus/")

        val allExcluded = PCDA_LAG + MARCH_SEMANTICS + GROUND_TRUTH_GAP
        var withTaxPage = 0
        var exact = 0
        val mismatches = mutableListOf<String>()
        val staleExclusions = mutableListOf<String>()

        for (id in ids) {
            val expected = CorpusFixtures.loadExpected(id)
            val tax = expected.taxAndSavings ?: continue
            if (tax.netTaxableIncome <= 0.0) continue
            // A null totalTaxPayable means TaxParserUtils' plausibility guard found no usable PCDA figure
            // to compare against (D3 fix) -- nothing to reproduce, not a parity result either way.
            val pcdaTotalTaxPayable = tax.totalTaxPayable ?: continue
            withTaxPage++

            val fy = TaxLedgerAggregator.computeFinancialYear(expected.year, expected.monthNum)
            val regime = detectRegime(id)
            val computed = computedTax(tax.netTaxableIncome, regime, fy)
            val isExact = kotlin.math.abs(computed - pcdaTotalTaxPayable) <= CorpusFixtures.TOLERANCE

            when {
                isExact && id in allExcluded -> staleExclusions += "$id now matches PCDA exactly — remove from its exclusion list."
                isExact -> exact++
                !isExact && id !in allExcluded ->
                    mismatches += "$id: fy=$fy regime=$regime computed=$computed pcda=$pcdaTotalTaxPayable"
                else -> Unit // expected mismatch inside a documented exclusion list
            }
        }

        println(
            "PcdaTaxParityTest scoreboard: $withTaxPage with a tax page, $exact exact, " +
                "${allExcluded.size} excluded (${PCDA_LAG.size} PCDA-lag / " +
                "${MARCH_SEMANTICS.size} march-semantics / ${GROUND_TRUTH_GAP.size} ground-truth-gap), " +
                "${mismatches.size} unexplained mismatch(es).",
        )

        val problems = mismatches + staleExclusions
        assertTrue(
            problems.isEmpty(),
            "PCDA tax parity ($withTaxPage fixtures, ${allExcluded.size} excluded):\n" + problems.joinToString("\n"),
        )
    }

    @Test
    fun exclusionListSizesAreExplicit() {
        assertTrue(PCDA_LAG.size == 8, "PCDA_LAG size drifted from 8 — update deliberately, not silently.")
        assertTrue(MARCH_SEMANTICS.size == 2, "MARCH_SEMANTICS size drifted from 2 — update deliberately, not silently.")
        assertTrue(GROUND_TRUTH_GAP.size == 1, "GROUND_TRUTH_GAP size drifted from 1 — update deliberately, not silently.")
    }

    @Test
    fun apr2026ReproducesPcdaHeadlineFigureExactly() {
        // The plan's flagship example: PCDA page 4 prints "10. Total Tax Payable 603822" on netTaxableIncome
        // 3412740 (New Tax Regime, FY2026-27). Asserted two ways: against the plan's documented ground
        // truth directly, and against the committed 04_apr_2026 fixture -- D3-corrupted (176068) before
        // Phase 4's guard fix + re-capture, now carrying the real, verified PCDA figure.
        val computed = computedTax(3412740.0, TaxRegime.NEW, "2026-27")
        assertTrue(kotlin.math.abs(computed - 603822.0) <= CorpusFixtures.TOLERANCE, "Apr 2026: expected 603822, got $computed")

        val expected = CorpusFixtures.loadExpected("04_apr_2026")
        assertTrue(
            expected.taxAndSavings?.totalTaxPayable == 603822.0,
            "04_apr_2026 fixture: expected totalTaxPayable 603822, got ${expected.taxAndSavings?.totalTaxPayable}",
        )
    }

    @Test
    fun jan2026ReproducesPcdaHeadlineFigureExactly() {
        // Plan Phase 1 acceptance: "Jan 2026 -> Rs 5,93,097 exactly". 01_jan_2026's own totalTaxPayable
        // field is uncorrupted, so this asserts directly against the committed fixture.
        val expected = CorpusFixtures.loadExpected("01_jan_2026")
        val tax = expected.taxAndSavings!!
        val fy = TaxLedgerAggregator.computeFinancialYear(expected.year, expected.monthNum)
        val computed = computedTax(tax.netTaxableIncome, detectRegime("01_jan_2026"), fy)
        assertTrue(kotlin.math.abs(computed - 593097.0) <= CorpusFixtures.TOLERANCE, "Jan 2026: expected 593097, got $computed")
    }
}
