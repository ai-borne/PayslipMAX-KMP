package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.domain.Part2Limits

/**
 * The size a stored page gets: one rule for both platforms, so the long edge is at most `longEdge` pixels on either, the shape is kept
 * and a small page is never enlarged. All arithmetic is in Long: a width read from a file can be anything an Int holds.
 */
object PageGeometry {
    /** The target width and height, or null when either is not positive. */
    fun fit(
        width: Int,
        height: Int,
        longEdge: Int,
    ): Pair<Int, Int>? = if (width <= 0 || height <= 0) null else fitPositive(width, height, longEdge)

    /** Whether an image of this size may be decoded at all (`Part2Limits.PAGE_PIXELS_MAX`). Long arithmetic: no pair of Ints can wrap the product. */
    fun pixelsWithinLimit(
        width: Int,
        height: Int,
    ): Boolean = width.toLong() * height <= Part2Limits.PAGE_PIXELS_MAX

    /** [fit] for sizes already known to be positive (a decoded bitmap's): no null to handle. */
    fun fitPositive(
        width: Int,
        height: Int,
        longEdge: Int,
    ): Pair<Int, Int> {
        val long = maxOf(width, height).toLong()
        if (long <= longEdge) return width to height
        return scaled(width, longEdge, long) to scaled(height, longEdge, long)
    }

    /** How much a decoder may shrink while reading (1, 2, 4, ...): the most that still leaves the long edge at `longEdge` or more. */
    fun sampleSize(
        width: Int,
        height: Int,
        longEdge: Int,
    ): Int {
        val long = maxOf(width, height).toLong()
        var sample = 1
        while (long / (sample * 2L) >= longEdge) sample *= 2
        return sample
    }

    private fun scaled(
        side: Int,
        longEdge: Int,
        long: Long,
    ): Int = maxOf(1L, (side.toLong() * longEdge * 2 + long) / (2 * long)).toInt()
}

/** Finds the best JPEG quality that makes a page fit its size cap. The codec is a function, so the rule is the same on both platforms. */
object JpegFit {
    /**
     * Tries [start], then each [step] lower, then the [floor] itself, and returns the first result of at most [maxBytes] (a null or empty result is no fit); null when even
     * the floor is too large (the page is then refused, not degraded further).
     */
    fun fit(
        maxBytes: Int,
        start: Int,
        step: Int,
        floor: Int,
        compress: (Int) -> ByteArray?,
    ): ByteArray? {
        val qualities = generateSequence(start) { it - step }.takeWhile { it > floor }.toList() + floor
        return qualities.firstNotNullOfOrNull { quality -> compress(quality)?.takeIf { it.isNotEmpty() && it.size <= maxBytes } }
    }
}
