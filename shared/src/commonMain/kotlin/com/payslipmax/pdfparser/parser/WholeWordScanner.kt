package com.payslipmax.pdfparser.parser

private fun Char.isAsciiWordChar(): Boolean = (this in 'a'..'z') || (this in 'A'..'Z') || (this in '0'..'9')

/**
 * Case-insensitive literal search where the match isn't flanked by an ASCII letter/digit - same
 * boundary semantics as the old (?<![a-zA-Z0-9])word(?![a-zA-Z0-9]) regex, without a regex engine
 * (avoids Kotlin/Native's slow lookaround handling; see docs/AI_INSIGHTS_PIPELINE.md).
 */
internal fun findWholeWordIgnoreCase(
    text: String,
    word: String,
    startIndex: Int = 0,
): IntRange? {
    if (word.isEmpty()) return null
    var from = startIndex
    while (from <= text.length - word.length) {
        val idx = text.indexOf(word, from, ignoreCase = true)
        if (idx < 0) return null
        val beforeOk = idx == 0 || !text[idx - 1].isAsciiWordChar()
        val afterIdx = idx + word.length
        val afterOk = afterIdx >= text.length || !text[afterIdx].isAsciiWordChar()
        if (beforeOk && afterOk) return idx until afterIdx
        from = idx + 1
    }
    return null
}

internal fun replaceWholeWordIgnoreCase(
    text: String,
    word: String,
    replacement: String,
): String {
    val sb = StringBuilder()
    var pos = 0
    while (true) {
        val range = findWholeWordIgnoreCase(text, word, pos) ?: break
        sb.append(text, pos, range.first)
        sb.append(replacement)
        pos = range.last + 1
    }
    sb.append(text, pos, text.length)
    return sb.toString()
}

private val totalsNumberTail = Regex("\\s*[:\\-–]?\\s*(?:Rs\\.?\\s*)?(\\d+)")

/**
 * Finds `key` as a whole word, then requires the numeric tail to start immediately after it
 * (only whitespace/separator/`Rs` in between) - same adjacency the old combined lookaround regex
 * enforced. If a given occurrence of `key` isn't immediately followed by a number, keeps scanning
 * for a later occurrence, mirroring the old regex's find()-over-the-whole-pattern behavior.
 */
internal fun findKeyedNumber(
    text: String,
    key: String,
): Double? {
    var searchFrom = 0
    while (true) {
        val keyRange = findWholeWordIgnoreCase(text, key, searchFrom) ?: return null
        val tailStart = keyRange.last + 1
        val tailMatch = totalsNumberTail.find(text, tailStart)
        if (tailMatch != null && tailMatch.range.first == tailStart) {
            return tailMatch.groupValues[1].toDoubleOrNull()
        }
        searchFrom = keyRange.last + 1
    }
}
