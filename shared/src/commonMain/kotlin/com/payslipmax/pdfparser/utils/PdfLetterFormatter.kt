package com.payslipmax.pdfparser.utils

/**
 * A4 page geometry for the generated representation PDF, expressed in PostScript points (1/72 inch)
 * so both the Android (`PdfDocument`) and iOS (`UIGraphicsPDFRenderer`) actuals lay out identically.
 * This is the single source of truth for page size, margins, and typography.
 */
object PdfLayoutSpec {
    const val PAGE_WIDTH = 595 // A4 width  @72dpi
    const val PAGE_HEIGHT = 842 // A4 height @72dpi
    const val MARGIN = 48
    const val TITLE_FONT_SIZE = 14
    const val BODY_FONT_SIZE = 11
    const val LINE_HEIGHT = 16

    val contentWidth: Int get() = PAGE_WIDTH - (2 * MARGIN)
    val contentBottom: Int get() = PAGE_HEIGHT - MARGIN
}

/** One laid-out source line of the letter, before platform-side width wrapping. */
data class PdfLine(
    val text: String,
    val isTitle: Boolean,
)

/**
 * Pure, platform-agnostic layout helpers for the representation PDF. The width-dependent word wrap
 * ([wrapLine]) takes a `measure` lambda so the greedy algorithm is shared and unit-testable while the
 * actual font metrics stay platform-side (Android `Paint`, iOS `sizeWithAttributes`).
 */
object PdfLetterFormatter {
    private const val FILE_PREFIX = "PCDA_Representation_"
    private const val FILE_EXTENSION = ".pdf"

    /** Ordered content: a bold title, a blank spacer, then the letter body split on newlines. */
    fun contentLines(
        title: String,
        bodyText: String,
    ): List<PdfLine> =
        buildList {
            add(PdfLine(title, isTitle = true))
            add(PdfLine("", isTitle = false))
            bodyText.split("\n").forEach { add(PdfLine(it, isTitle = false)) }
        }

    /** Filesystem-safe, stable name derived from the dispute month (e.g. `PCDA_Representation_02_2026.pdf`). */
    fun fileName(disputeMonth: String): String {
        val slug = disputeMonth.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
        return "$FILE_PREFIX$slug$FILE_EXTENSION"
    }

    /**
     * Greedy word wrap of [text] to [maxWidth] using [measure] (width of a candidate string). A blank
     * line is preserved as a single empty line so paragraph spacing in the source survives. A word wider
     * than a line stays whole on its own line, unless [breakLongWords] cuts it into lines that fit (never
     * between the two halves of a surrogate pair), for text that must not run off the page.
     */
    fun wrapLine(
        text: String,
        maxWidth: Double,
        breakLongWords: Boolean = false,
        measure: (String) -> Double,
    ): List<String> {
        if (text.isBlank()) return listOf("")
        val out = mutableListOf<String>()
        var current = ""
        for (word in text.split(" ")) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (measure(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) out.add(current)
                current = word
                while (breakLongWords && measure(current) > maxWidth) {
                    val cut = fittingLength(current, maxWidth, measure)
                    out.add(current.substring(0, cut))
                    current = current.substring(cut)
                }
            }
        }
        if (current.isNotEmpty()) out.add(current)
        return out
    }

    /** The longest prefix of [word] (at least one character, and never ending inside a surrogate pair) that fits [maxWidth]; widths only grow with length, so a binary search finds it. */
    private fun fittingLength(
        word: String,
        maxWidth: Double,
        measure: (String) -> Double,
    ): Int {
        var low = 1
        var high = word.length - 1
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (measure(word.substring(0, middle)) <= maxWidth) low = middle else high = middle - 1
        }
        return if (low > 1 && word[low - 1].isHighSurrogate()) low - 1 else low
    }
}
