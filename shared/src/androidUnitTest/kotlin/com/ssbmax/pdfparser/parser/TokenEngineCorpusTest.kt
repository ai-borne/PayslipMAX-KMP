package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.parser.corpus.CorpusExpected
import com.ssbmax.pdfparser.parser.corpus.CorpusFixtures
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Phase 3 — the always-on proof that the pure-common token engine
 * (`GridReconstructor → RowPairing → TokenTableClassifier`) reconstructs the right line items from the
 * real, de-identified `<id>.tokens.json` fixtures, with no device and no PII.
 *
 * It asserts only the **unambiguous, single-row** fields (basic pay, DA, MSP and the core deductions).
 * Cross-column reversals, arrears routing, misc residuals and ledger carry-over are the arithmetic
 * solver's job at the Phase 4 cut-over, so they are intentionally out of scope here — this test guards
 * the classification core, not the final reconciliation.
 *
 * ### Quarantine
 * The engine extracts the *raw* on-page amounts. For a few 2022–2023 slips the committed ground truth
 * bakes in the legacy [DynamicSpatialParser.applyHistoricalOverrides] micro-patches (e.g. basicPay +14,
 * DA +29, MSP +24 for Apr-2022; DA +58 for Apr-2023) that round-reconcile those older layouts. Those
 * hardcoded patches are exactly what the Phase 4 arithmetic solver will replace, so the affected
 * `<fixture>:<field>` pairs are quarantined here with the expected delta. The test still asserts the
 * raw value is correct *to within that documented override*, and flags any quarantine entry that
 * starts matching exactly (so the list never rots once Phase 4 lands).
 */
class TokenEngineCorpusTest {
    /** Standardized credit keys that always appear as a plain (label, amount) row. */
    private val creditChecks = listOf("basicPay", "dearnessAllowance", "militaryServicePay")

    /** Standardized debit keys that always appear as a plain (label, amount) row. */
    private val debitChecks = listOf("dsopSubscription", "agif", "incomeTax", "educationCess")

    /** "<fixtureId>:<field>" → rupee delta the legacy historical override adds on top of the raw token value. */
    private val quarantine: Map<String, Double> =
        mapOf(
            "04_april_2022:basicPay" to 14.0,
            "04_april_2022:dearnessAllowance" to 29.0,
            "04_april_2022:militaryServicePay" to 24.0,
            "04_apr_2023:dearnessAllowance" to 58.0,
        )

    @Test
    fun engineRecoversCoreLineItemsFromAllTokenFixtures() {
        val ids = CorpusFixtures.loadIndex()
        assertTrue(ids.isNotEmpty(), "Corpus index is empty — token fixtures missing")

        val problems = mutableListOf<String>()
        for (id in ids) {
            val tokens = CorpusFixtures.loadTokens(id)
            val expected = CorpusFixtures.loadExpected(id)
            val table = TokenTableClassifier.classify(tokens.tableTokens)

            // Every officer payslip carries Basic Pay — the engine must always find a non-zero value.
            if (expected.earnings.basicPay > 0.0 && (table.standardizedCredits()["basicPay"] ?: 0.0) <= 0.0) {
                problems += "$id: basicPay not recovered"
            }

            problems += check(id, "credit", creditChecks, expectedCredit(expected), table.standardizedCredits())
            problems += check(id, "debit", debitChecks, expectedDebit(expected), table.standardizedDebits())
        }

        assertTrue(
            problems.isEmpty(),
            "Token engine corpus problems (${ids.size} fixtures, ${quarantine.size} quarantined):\n" +
                problems.joinToString("\n"),
        )
    }

    private fun expectedCredit(e: CorpusExpected) =
        mapOf(
            "basicPay" to e.earnings.basicPay,
            "dearnessAllowance" to e.earnings.dearnessAllowance,
            "militaryServicePay" to e.earnings.militaryServicePay,
        )

    private fun expectedDebit(e: CorpusExpected) =
        mapOf(
            "dsopSubscription" to e.deductions.dsopSubscription,
            "agif" to e.deductions.agif,
            "incomeTax" to e.deductions.incomeTax,
            "educationCess" to e.deductions.educationCess,
        )

    private fun check(
        id: String,
        side: String,
        keys: List<String>,
        expected: Map<String, Double>,
        actual: Map<String, Double>,
    ): List<String> {
        val out = mutableListOf<String>()
        for (key in keys) {
            val exp = expected[key] ?: 0.0
            if (exp <= 0.0) continue // only assert fields the ground truth actually carries
            val got = actual[key] ?: 0.0
            val override = quarantine["$id:$key"]
            if (override != null) {
                // Raw token value must equal ground-truth minus the legacy override; if it matches
                // ground truth exactly, Phase 4 has removed the patch and the entry should be dropped.
                if (abs(got - exp) <= CorpusFixtures.TOLERANCE) {
                    out += "$id: $side.$key now matches exactly — remove from quarantine (override resolved)."
                } else if (abs((got + override) - exp) > CorpusFixtures.TOLERANCE) {
                    out += "$id: $side.$key expected ${exp - override} (raw, override $override), got $got"
                }
            } else if (abs(got - exp) > CorpusFixtures.TOLERANCE) {
                out += "$id: $side.$key expected $exp, got $got"
            }
        }
        return out
    }
}
