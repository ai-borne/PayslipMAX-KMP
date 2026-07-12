package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 2 of the phantom-numbers sprint — [RawLabelNoiseFilter.isDatePlaceOnlyNoise], the backstop
 * for phantom-shaped rows that survive Phase 1's [VerticalBandFilter] because they sit *inside* the
 * learned table body (indistinguishable from real rows by Y alone). Driven entirely from hand-built
 * [LabelAmount] rows via [TokenTableClassifier.classifyPairs] — no tokens, no grid, no fixtures.
 */
class PlausibilityBackstopTest {
    /** Real body rows: BPAY/DA/MSP/DSOP, all cleanly matched to a known key. */
    private fun bodyRows() =
        listOf(
            LabelAmount("BPAY", 140500.0, 44f, 145f, 261f),
            LabelAmount("DA", 71760.0, 44f, 145f, 281f),
            LabelAmount("MSP", 15500.0, 44f, 145f, 301f),
            LabelAmount("DSOP", 40000.0, 186f, 280f, 321f),
        )

    @Test
    fun datePlaceOnlyLabelInsideBodyIsDropped() {
        // "Page No Dated" is pure administrative filler (all three words in
        // PayslipPatternConfig.invalidEntireKeys) — not a real PCDA code — sitting between two real
        // rows at the debit column's own x-band, so no geometry rule catches it.
        val noise = LabelAmount("Page No Dated", 45.0, 186f, 280f, 291f)
        val table = TokenTableClassifier.classifyPairs(bodyRows() + listOf(noise))

        assertTrue(table.debits.none { it.amount == 45.0 }, "administrative-filler label must not appear as a debit line item")
        assertTrue(table.rawDeductions().keys.none { it == "Page No Dated" }, "administrative-filler label must not leak into rawDeductions")
        assertEquals(40000.0, table.standardizedDebits()["dsopSubscription"], "real body rows are unaffected")
    }

    @Test
    fun partIiOrderFragmentInsideBodyIsDropped() {
        // Mirrors the real "Details of Transactions" narrative shape (a Part II Order note) that
        // motivated this sprint's Phase 2 scope — same PCDA-boilerplate words, synthetic amount.
        val noise = LabelAmount("Part II Order", 1936.0, 44f, 145f, 271f)
        val table = TokenTableClassifier.classifyPairs(bodyRows() + listOf(noise))

        assertTrue(table.credits.none { it.amount == 1936.0 }, "Part II Order narrative fragment must not appear as a credit line item")
        assertEquals(140500.0, table.standardizedCredits()["basicPay"], "real body rows are unaffected")
    }

    @Test
    fun tptadaYearShapedRealAllowanceSurvives() {
        // Guardrail (D2): TPTADA is a real, cleanly-matched credit key whose value legitimately
        // drifts through the bare-year range (1908 in Dec 2024, 2088 in Jan 2026). Value-shape must
        // never be a standalone trigger — only a structurally-noise label backs a drop, and a clean
        // key match is never even routed through the noise predicate.
        val table1 = TokenTableClassifier.classifyPairs(bodyRows() + listOf(LabelAmount("TPTADA", 1908.0, 44f, 145f, 271f)))
        val table2 = TokenTableClassifier.classifyPairs(bodyRows() + listOf(LabelAmount("TPTADA", 2088.0, 44f, 145f, 271f)))

        assertEquals(1908.0, table1.standardizedCredits()["transportAllowanceDa"], "TPTADA=1908 must survive as a real credit")
        assertEquals(2088.0, table2.standardizedCredits()["transportAllowanceDa"], "TPTADA=2088 must survive as a real credit")
    }

    @Test
    fun unknownLabelWithYearShapedValueButRealContentIsKept() {
        // Value-shape alone (a number in the year/pin range) must never trigger a drop — only a
        // genuinely structurally-noise label does. An unrecognized but real-looking short code is
        // kept, routed to the raw channel per the sprint's keep-and-flag governing principle.
        val table = TokenTableClassifier.classifyPairs(bodyRows() + listOf(LabelAmount("XYZALW", 1925.0, 44f, 145f, 271f)))

        assertEquals(1925.0, table.rawCredits()["XYZALW"], "unmatched but structurally real label must still route to rawEarnings")
    }
}
