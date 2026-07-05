package com.payslipmax.pdfparser.parser.diff

import com.payslipmax.pdfparser.parser.PositionedToken
import kotlin.math.abs

enum class DivergenceCategory {
    /** Same token text; x, y, or height differs beyond [TokenDiff.GEOMETRY_THRESHOLD]. */
    GEOMETRY_OFFSET,

    /** Token text present in both lists but appears in a different relative position. */
    READING_ORDER,

    /** One side has a single token whose text equals two consecutive tokens joined with a space on the other side. */
    WORD_SPLIT,

    /** Token present on one side with no matching counterpart on the other. */
    MISSING_TOKEN,
}

/**
 * A single entry in a [TokenDiffReport]. [dx]/[dy]/[dHeight] are ios − android deltas and are
 * meaningful only when [categories] contains [DivergenceCategory.GEOMETRY_OFFSET]; they are 0f
 * for all other divergence types.
 */
data class TokenDelta(
    val index: Int,
    val androidText: String?,
    val iosText: String?,
    val dx: Float,
    val dy: Float,
    val dHeight: Float,
    val categories: Set<DivergenceCategory>,
)

/** Summary returned by [TokenDiff.compare]. */
data class TokenDiffReport(
    val androidCount: Int,
    val iosCount: Int,
    val deltas: List<TokenDelta>,
    val categoryCounts: Map<DivergenceCategory, Int>,
) {
    val dominantCategory: DivergenceCategory?
        get() = categoryCounts.entries.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
}

/**
 * Pure, platform-independent helper that walks two [PositionedToken] lists and classifies every
 * divergence into one of the four [DivergenceCategory] values. Used by both [TokenDiffTest] (unit)
 * and [TokenParityDiffTest] (corpus). Stays under 150 lines; no I/O, no state.
 */
object TokenDiff {
    /** Points — shifts beyond this value in x, y, or height are flagged as GEOMETRY_OFFSET. */
    const val GEOMETRY_THRESHOLD = 2f

    /** How far ahead to look when detecting READING_ORDER vs MISSING_TOKEN. */
    private const val LOOKAHEAD = 8

    fun compare(
        android: List<PositionedToken>,
        ios: List<PositionedToken>,
    ): TokenDiffReport {
        val deltas = mutableListOf<TokenDelta>()
        var i = 0
        var j = 0
        var idx = 0

        while (i < android.size || j < ios.size) {
            val a = android.getOrNull(i)
            val b = ios.getOrNull(j)

            when {
                a == null -> {
                    deltas += TokenDelta(idx++, null, b!!.text, 0f, 0f, 0f, setOf(DivergenceCategory.MISSING_TOKEN))
                    j++
                }

                b == null -> {
                    deltas += TokenDelta(idx++, a.text, null, 0f, 0f, 0f, setOf(DivergenceCategory.MISSING_TOKEN))
                    i++
                }

                a.text == b.text -> {
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val dh = b.height - a.height
                    if (abs(dx) > GEOMETRY_THRESHOLD || abs(dy) > GEOMETRY_THRESHOLD || abs(dh) > GEOMETRY_THRESHOLD) {
                        deltas += TokenDelta(idx, a.text, b.text, dx, dy, dh, setOf(DivergenceCategory.GEOMETRY_OFFSET))
                    }
                    i++
                    j++
                    idx++
                }

                else -> {
                    val delta = detectSplitOrOrder(android, ios, i, j, idx, a, b)
                    deltas += delta.first
                    i += delta.second
                    j += delta.third
                    idx++
                }
            }
        }

        val counts =
            DivergenceCategory.entries.associateWith { cat ->
                deltas.count { cat in it.categories }
            }
        return TokenDiffReport(android.size, ios.size, deltas, counts)
    }

    /**
     * Returns (delta, androidAdvance, iosAdvance). Called only when a.text != b.text.
     * Checks (in order): android-merged split, ios-merged split, reading-order look-ahead,
     * then falls back to MISSING_TOKEN.
     */
    private fun detectSplitOrOrder(
        android: List<PositionedToken>,
        ios: List<PositionedToken>,
        i: Int,
        j: Int,
        idx: Int,
        a: PositionedToken,
        b: PositionedToken,
    ): Triple<TokenDelta, Int, Int> {
        val nextB = ios.getOrNull(j + 1)
        if (nextB != null && isSplit(a.text, b.text, nextB.text)) {
            return Triple(
                TokenDelta(idx, a.text, "${b.text}${nextB.text}", 0f, 0f, 0f, setOf(DivergenceCategory.WORD_SPLIT)),
                1,
                2,
            )
        }

        val nextA = android.getOrNull(i + 1)
        if (nextA != null && isSplit(b.text, a.text, nextA.text)) {
            return Triple(
                TokenDelta(idx, "${a.text}${nextA.text}", b.text, 0f, 0f, 0f, setOf(DivergenceCategory.WORD_SPLIT)),
                2,
                1,
            )
        }

        val aInIosAhead = ios.subList(j + 1, minOf(j + 1 + LOOKAHEAD, ios.size)).any { it.text == a.text }
        val bInAndroidAhead = android.subList(i + 1, minOf(i + 1 + LOOKAHEAD, android.size)).any { it.text == b.text }
        val cat = if (aInIosAhead || bInAndroidAhead) DivergenceCategory.READING_ORDER else DivergenceCategory.MISSING_TOKEN
        return Triple(
            TokenDelta(idx, a.text, b.text, 0f, 0f, 0f, setOf(cat)),
            1,
            1,
        )
    }

    /** True when [merged] equals [first] + [second] with or without a space separator. */
    private fun isSplit(
        merged: String,
        first: String,
        second: String,
    ): Boolean =
        merged == "$first $second" || merged == "$first$second"
}
