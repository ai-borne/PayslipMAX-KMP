package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 1 of the phantom-numbers sprint (see `docs/AI_INSIGHTS_PIPELINE.md`) — pure geometric unit
 * tests for [VerticalBandFilter], driven entirely from hand-built [LabelAmount] rows (no tokens, no
 * grid, no fixtures). Mirrors a real PCDA page: a header block above the table body, four real line
 * items (the body), and a totals row followed by footer text.
 *
 * The "Gross Pay"/"Total Deductions" totals-row label is already excluded from
 * [TokenTableClassifier]'s candidate list upstream by [PayslipPatternConfig.blocklist] before it would
 * ever reach [VerticalBandFilter.apply] in production — so tests pass the totals row into
 * [VerticalBandFilter.computeBounds] (which locates it by *value*, independent of the blocklist) but
 * never into [VerticalBandFilter.apply]'s candidate list, mirroring the real call order.
 */
class VerticalBandFilterTest {
    /** Real body rows: BPAY/DA/MSP/DSOP, all cleanly matched to a known key. */
    private fun bodyRows() =
        listOf(
            LabelAmount("BPAY", 140500.0, 44f, 145f, 261f),
            LabelAmount("DA", 71760.0, 44f, 145f, 281f),
            LabelAmount("MSP", 15500.0, 44f, 145f, 301f),
            LabelAmount("DSOP", 40000.0, 186f, 280f, 321f),
        )

    private fun bodyRowYs() = bodyRows().map { it.centerY }

    @Test
    fun headerRowAboveFirstMatchedKeyIsDropped() {
        val headerYear = LabelAmount("Statement of Account 2015", 2015.0, 300f, 300f, 25f)
        val headerPin = LabelAmount("Glibar Maidan Pune", 411001.0, 300f, 300f, 40f)
        val rows = listOf(headerYear, headerPin) + bodyRows()

        val bounds = VerticalBandFilter.computeBounds(rows, bodyRowYs(), grossPay = 0.0, totalDeductions = 0.0)
        val kept = VerticalBandFilter.apply(rows, LabelAmount::centerY, bounds)

        assertTrue(headerYear !in kept, "statement-title year above BPAY must be dropped")
        assertTrue(headerPin !in kept, "address pin code above BPAY must be dropped")
        assertEquals(bodyRows(), kept, "all real body rows survive untouched")
    }

    @Test
    fun footerRowBelowTotalsRowIsDropped() {
        val totalsRow = LabelAmount("Gross Pay", 267760.0, 44f, 145f, 360f)
        val footerYear = LabelAmount("Next Increment Date:01/01/2027", 2027.0, 44f, 145f, 400f)
        // The totals row's own label is already excluded upstream by PayslipPatternConfig.blocklist —
        // it never reaches `apply`'s candidate list — but its *value* is what locates the bottom bound.
        val allRows = bodyRows() + listOf(totalsRow, footerYear)
        val candidateRows = bodyRows() + listOf(footerYear)

        val bounds = VerticalBandFilter.computeBounds(allRows, bodyRowYs(), grossPay = 267760.0, totalDeductions = 0.0)
        val kept = VerticalBandFilter.apply(candidateRows, LabelAmount::centerY, bounds)

        assertEquals(360f, bounds.bottom, "bottom bound must lock onto the printed Gross Pay row by value")
        assertTrue(footerYear !in kept, "Next Increment Date year below the totals row must be dropped")
        assertEquals(bodyRows(), kept, "all real body rows survive untouched")
    }

    @Test
    fun realRowsSurviveBothBoundsSimultaneously() {
        val headerPin = LabelAmount("Glibar Maidan Pune", 411001.0, 300f, 300f, 25f)
        val totalsRow = LabelAmount("Gross Pay", 267760.0, 44f, 145f, 360f)
        val footerYear = LabelAmount("Next Increment Date:01/01/2027", 2027.0, 44f, 145f, 400f)
        val allRows = listOf(headerPin) + bodyRows() + listOf(totalsRow, footerYear)
        val candidateRows = listOf(headerPin) + bodyRows() + listOf(footerYear)

        val bounds = VerticalBandFilter.computeBounds(allRows, bodyRowYs(), grossPay = 267760.0, totalDeductions = 0.0)
        val kept = VerticalBandFilter.apply(candidateRows, LabelAmount::centerY, bounds)

        assertEquals(bodyRows(), kept)
    }

    @Test
    fun boundsAreLearnedNotHardcodedUnderVerticalTranslation() {
        val offset = 1000f
        val headerPin = LabelAmount("Glibar Maidan Pune", 411001.0, 300f, 300f, 25f + offset)
        val totalsRow = LabelAmount("Gross Pay", 267760.0, 44f, 145f, 360f + offset)
        val footerYear = LabelAmount("Next Increment Date:01/01/2027", 2027.0, 44f, 145f, 400f + offset)
        val shiftedBody = bodyRows().map { it.copy(centerY = it.centerY + offset) }
        val allRows = listOf(headerPin) + shiftedBody + listOf(totalsRow, footerYear)
        val candidateRows = listOf(headerPin) + shiftedBody + listOf(footerYear)

        val bounds =
            VerticalBandFilter.computeBounds(
                allRows,
                shiftedBody.map { it.centerY },
                grossPay = 267760.0,
                totalDeductions = 0.0,
            )
        val kept = VerticalBandFilter.apply(candidateRows, LabelAmount::centerY, bounds)

        assertEquals(360f + offset, bounds.bottom, "bound tracks the shifted totals row, not a fixed y")
        assertEquals(shiftedBody, kept, "same keep/drop outcome after a uniform vertical shift")
    }

    @Test
    fun noCleanMatchAndNoTotalsMatchKeepsEverything() {
        // No signal to learn either bound from: both must stay null, and nothing is dropped —
        // the sprint's governing keep-and-flag principle.
        val rows = listOf(LabelAmount("Unknown Row", 500.0, 44f, 145f, 100f))

        val bounds = VerticalBandFilter.computeBounds(rows, emptyList(), grossPay = 0.0, totalDeductions = 0.0)

        assertNull(bounds.top)
        assertNull(bounds.bottom)
        assertEquals(rows, VerticalBandFilter.apply(rows, LabelAmount::centerY, bounds))
    }

    @Test
    fun missingTotalsFallsBackToLastCleanMatchPlusMedianRowHeight() {
        // Footer parse failed (grossPay/totalDeductions unavailable): bound by geometry alone.
        val strayBelow = LabelAmount("Stray", 999.0, 44f, 145f, 500f)
        val rows = bodyRows() + listOf(strayBelow)

        val bounds = VerticalBandFilter.computeBounds(rows, bodyRowYs(), grossPay = 0.0, totalDeductions = 0.0)
        val kept = VerticalBandFilter.apply(rows, LabelAmount::centerY, bounds)

        assertEquals(341f, bounds.bottom, "fallback = last clean match (321) + median row gap (20)")
        assertTrue(strayBelow !in kept)
        assertEquals(bodyRows(), kept)
    }

    @Test
    fun dropsAreReportedForExplainability() {
        val headerPin = LabelAmount("Glibar Maidan Pune", 411001.0, 300f, 300f, 25f)
        val rows = listOf(headerPin) + bodyRows()
        val bounds = VerticalBandFilter.computeBounds(rows, bodyRowYs(), grossPay = 0.0, totalDeductions = 0.0)

        val dropped = mutableListOf<LabelAmount>()
        VerticalBandFilter.apply(rows, LabelAmount::centerY, bounds) { dropped.add(it) }

        assertEquals(listOf(headerPin), dropped)
    }
}
