package com.payslipmax.pdfparser.tax

import com.payslipmax.pdfparser.domain.TaxRegime
import com.payslipmax.pdfparser.insights.DualRegimeEngine
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
 * Four documented, data-driven exclusion categories (never `@Ignore`) -- a fixture leaving its list
 * silently is a bug in this test, not a reason to delete the check, so [staleExclusions] fails loudly.
 */
class PcdaTaxParityTest {
    /** D3: `totalTaxPayable == round(grossSalaryYtd * 0.30)`, the sanity-guard bug destroying PCDA's real
     * figure (TaxParserUtils.kt). Deleted by Phase 4 once the 17-vs-13 fixture regeneration lands (see
     * this test's KDoc changelog note below for why the committed corpus currently has 13, not 17). */
    private val CORRUPTED_TOTAL_TAX =
        setOf(
            "03_mar_18", "03_mar_2019", "03_mar_2020", "03_mar_2021", "03_mar_2023",
            "03_mar_2024", "03_mar_2025", "03_mar_2026", "04_apr_2020", "04_apr_2021",
            "04_apr_2023", "04_apr_2026", "mar_16",
        )

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

    /** March fixtures (not already in [CORRUPTED_TOTAL_TAX]) where `grossSalaryYtd` appears to print only
     * the current month's gross rather than a cumulative YTD figure -- excluded pending the Phase 4
     * investigation rather than silently trusted (Risk register: "fixtures stay excluded until
     * understood -- not silently included"). `03_march_2022` was provisionally added here too but this
     * test's own staleness check proved it exact (net taxable 44,333 is below the exemption threshold,
     * so it never exercises the grossSalaryYtd anomaly this list exists for) -- removed per that check. */
    private val MARCH_SEMANTICS = setOf("03_mar_2017")

    /** Not anticipated by the plan: apr_14's `totalTaxableIncome`/`standardDeduction`/`totalTaxPayable`/
     * `cessDeductedYtd` are all 0 despite a populated `netTaxableIncome`, i.e. the tax-page ground truth
     * was never fully captured for this fixture (predates full tax-page field capture). Not a computation
     * defect -- there is no real PCDA figure here to reproduce. Surfaced during Phase 0 evidence-gathering
     * as a 4th category the plan's three lists didn't anticipate. */
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
        val rules = TaxRuleKnowledgeBase.getRulesForFy(fy)
        return if (regime == TaxRegime.NEW) {
            val gross = netTaxableIncome + rules.standardDeductionNew
            DualRegimeEngine.calculateNewRegimeTax(gross, fy).baseTax
        } else {
            val gross = netTaxableIncome + rules.standardDeductionOld
            DualRegimeEngine.calculateOldRegimeTax(gross, 0.0, fy).baseTax
        }
    }

    @Test
    fun productionEngineReproducesPcdaAcrossCorpus() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — fixtures missing from resources/corpus/")

        val allExcluded = CORRUPTED_TOTAL_TAX + PCDA_LAG + MARCH_SEMANTICS + GROUND_TRUTH_GAP
        var withTaxPage = 0
        var exact = 0
        val mismatches = mutableListOf<String>()
        val staleExclusions = mutableListOf<String>()

        for (id in ids) {
            val expected = CorpusFixtures.loadExpected(id)
            val tax = expected.taxAndSavings ?: continue
            if (tax.netTaxableIncome <= 0.0) continue
            withTaxPage++

            val fy = TaxLedgerAggregator.computeFinancialYear(expected.year, expected.monthNum)
            val regime = detectRegime(id)
            val computed = computedTax(tax.netTaxableIncome, regime, fy)
            val isExact = kotlin.math.abs(computed - tax.totalTaxPayable) <= CorpusFixtures.TOLERANCE

            when {
                isExact && id in allExcluded -> staleExclusions += "$id now matches PCDA exactly — remove from its exclusion list."
                isExact -> exact++
                !isExact && id !in allExcluded ->
                    mismatches += "$id: fy=$fy regime=$regime computed=$computed pcda=${tax.totalTaxPayable}"
                else -> Unit // expected mismatch inside a documented exclusion list
            }
        }

        println(
            "PcdaTaxParityTest scoreboard: $withTaxPage with a tax page, $exact exact, " +
                "${allExcluded.size} excluded (${CORRUPTED_TOTAL_TAX.size} corrupted / ${PCDA_LAG.size} PCDA-lag / " +
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
        assertTrue(CORRUPTED_TOTAL_TAX.size == 13, "CORRUPTED_TOTAL_TAX size drifted from 13 — update deliberately, not silently.")
        assertTrue(PCDA_LAG.size == 8, "PCDA_LAG size drifted from 8 — update deliberately, not silently.")
        assertTrue(MARCH_SEMANTICS.size == 1, "MARCH_SEMANTICS size drifted from 1 — update deliberately, not silently.")
        assertTrue(GROUND_TRUTH_GAP.size == 1, "GROUND_TRUTH_GAP size drifted from 1 — update deliberately, not silently.")
    }

    @Test
    fun apr2026ReproducesPcdaHeadlineFigureExactly() {
        // The plan's flagship example: PCDA page 4 prints "10. Total Tax Payable 603822" on netTaxableIncome
        // 3412740 (New Tax Regime, FY2026-27). The fixture's own totalTaxPayable is D3-corrupted (176068), so
        // this asserts against the plan's documented ground truth directly, not the corrupted fixture field.
        val computed = computedTax(3412740.0, TaxRegime.NEW, "2026-27")
        assertTrue(kotlin.math.abs(computed - 603822.0) <= CorpusFixtures.TOLERANCE, "Apr 2026: expected 603822, got $computed")
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
