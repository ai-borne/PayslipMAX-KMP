package com.payslipmax.pdfparser.parser.diff

import com.payslipmax.pdfparser.parser.PositionedToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenDiffTest {
    private fun tok(
        text: String,
        x: Float = 0f,
        y: Float = 0f,
        w: Float = 50f,
        h: Float = 12f,
    ) = PositionedToken(text = text, x = x, y = y, width = w, height = h)

    // ── No divergence ────────────────────────────────────────────────────────

    @Test
    fun `identical empty lists produce empty report`() {
        val report = TokenDiff.compare(emptyList(), emptyList())
        assertEquals(0, report.deltas.size)
        assertNull(report.dominantCategory)
    }

    @Test
    fun `identical token lists produce no deltas`() {
        val tokens = listOf(tok("Basic Pay", x = 10f, y = 20f), tok("12345", x = 200f, y = 20f))
        val report = TokenDiff.compare(tokens, tokens)
        assertTrue(report.deltas.isEmpty(), "Expected no deltas for identical lists")
    }

    // ── GEOMETRY_OFFSET ──────────────────────────────────────────────────────

    @Test
    fun `same text with large x-shift is classified as GEOMETRY_OFFSET`() {
        val android = listOf(tok("Basic Pay", x = 10f, y = 50f))
        val ios = listOf(tok("Basic Pay", x = 25f, y = 50f)) // dx = 15 > threshold

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        assertTrue(DivergenceCategory.GEOMETRY_OFFSET in report.deltas[0].categories)
        assertEquals(15f, report.deltas[0].dx, 0.01f)
        assertEquals(0f, report.deltas[0].dy, 0.01f)
    }

    @Test
    fun `same text with large y-shift is classified as GEOMETRY_OFFSET`() {
        val android = listOf(tok("12345", x = 200f, y = 100f))
        val ios = listOf(tok("12345", x = 200f, y = 115f)) // dy = 15 > threshold

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        assertTrue(DivergenceCategory.GEOMETRY_OFFSET in report.deltas[0].categories)
        assertEquals(0f, report.deltas[0].dx, 0.01f)
        assertEquals(15f, report.deltas[0].dy, 0.01f)
    }

    @Test
    fun `same text with large height difference is classified as GEOMETRY_OFFSET`() {
        val android = listOf(tok("ARR-", x = 10f, y = 50f, h = 12f))
        val ios = listOf(tok("ARR-", x = 10f, y = 50f, h = 20f)) // dHeight = 8 > threshold

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        assertTrue(DivergenceCategory.GEOMETRY_OFFSET in report.deltas[0].categories)
        assertEquals(8f, report.deltas[0].dHeight, 0.01f)
    }

    @Test
    fun `same text with small shift below threshold produces no delta`() {
        val android = listOf(tok("Pay", x = 10f, y = 50f))
        val ios = listOf(tok("Pay", x = 11f, y = 50f)) // dx = 1 <= threshold (2f)

        val report = TokenDiff.compare(android, ios)
        assertTrue(report.deltas.isEmpty(), "Sub-threshold shift should not produce a delta")
    }

    @Test
    fun `report captures per-token x y height deltas for GEOMETRY_OFFSET`() {
        val android =
            listOf(
                tok("A", x = 10f, y = 20f, h = 12f),
                tok("B", x = 100f, y = 20f, h = 12f),
            )
        val ios =
            listOf(
                // identical
                tok("A", x = 10f, y = 20f, h = 12f),
                // shifted
                tok("B", x = 120f, y = 25f, h = 14f),
            )

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        val delta = report.deltas[0]
        assertEquals("B", delta.androidText)
        assertEquals(20f, delta.dx, 0.01f)
        assertEquals(5f, delta.dy, 0.01f)
        assertEquals(2f, delta.dHeight, 0.01f)
    }

    // ── WORD_SPLIT ───────────────────────────────────────────────────────────

    @Test
    fun `android merged token split into two on iOS is classified as WORD_SPLIT`() {
        val android = listOf(tok("Basic Pay"))
        val ios = listOf(tok("Basic"), tok("Pay"))

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        assertTrue(DivergenceCategory.WORD_SPLIT in report.deltas[0].categories)
        assertEquals("Basic Pay", report.deltas[0].androidText)
    }

    @Test
    fun `two android tokens merged into one on iOS is classified as WORD_SPLIT`() {
        val android = listOf(tok("Basic"), tok("Pay"))
        val ios = listOf(tok("Basic Pay"))

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        assertTrue(DivergenceCategory.WORD_SPLIT in report.deltas[0].categories)
        assertEquals("Basic Pay", report.deltas[0].iosText)
    }

    @Test
    fun `ARR- prefix token split detection`() {
        val android = listOf(tok("ARR-12345"))
        val ios = listOf(tok("ARR-"), tok("12345"))

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        assertTrue(DivergenceCategory.WORD_SPLIT in report.deltas[0].categories)
    }

    // ── MISSING_TOKEN ────────────────────────────────────────────────────────

    @Test
    fun `extra iOS token with no android counterpart is classified as MISSING_TOKEN`() {
        val android = listOf(tok("Pay"))
        val ios = listOf(tok("Pay"), tok("Extra"))

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        assertTrue(DivergenceCategory.MISSING_TOKEN in report.deltas[0].categories)
        assertEquals("Extra", report.deltas[0].iosText)
        assertNull(report.deltas[0].androidText)
    }

    @Test
    fun `android token absent from iOS is classified as MISSING_TOKEN`() {
        val android = listOf(tok("Pay"), tok("Gone"))
        val ios = listOf(tok("Pay"))

        val report = TokenDiff.compare(android, ios)
        assertEquals(1, report.deltas.size)
        assertTrue(DivergenceCategory.MISSING_TOKEN in report.deltas[0].categories)
        assertEquals("Gone", report.deltas[0].androidText)
        assertNull(report.deltas[0].iosText)
    }

    // ── READING_ORDER ────────────────────────────────────────────────────────

    @Test
    fun `swapped token pair is classified as READING_ORDER`() {
        // Android: A, B, C  →  iOS: A, C, B
        val android = listOf(tok("A"), tok("B"), tok("C"))
        val ios = listOf(tok("A"), tok("C"), tok("B"))

        val report = TokenDiff.compare(android, ios)
        assertTrue(
            (report.categoryCounts[DivergenceCategory.READING_ORDER] ?: 0) > 0,
            "Expected at least one READING_ORDER divergence for swapped tokens",
        )
    }

    // ── dominantCategory ─────────────────────────────────────────────────────

    @Test
    fun `dominantCategory reflects the most frequent divergence type`() {
        val android = listOf(tok("A", x = 10f), tok("B", x = 50f), tok("C", x = 90f))
        val ios = listOf(tok("A", x = 30f), tok("B", x = 70f), tok("C", x = 110f))

        val report = TokenDiff.compare(android, ios)
        assertEquals(DivergenceCategory.GEOMETRY_OFFSET, report.dominantCategory)
    }

    @Test
    fun `categoryCounts sums divergences per category`() {
        val android = listOf(tok("A", x = 10f), tok("B", x = 50f))
        val ios = listOf(tok("A", x = 30f), tok("B", x = 70f))

        val report = TokenDiff.compare(android, ios)
        assertEquals(2, report.categoryCounts[DivergenceCategory.GEOMETRY_OFFSET])
        assertEquals(0, report.categoryCounts[DivergenceCategory.MISSING_TOKEN] ?: 0)
    }
}
