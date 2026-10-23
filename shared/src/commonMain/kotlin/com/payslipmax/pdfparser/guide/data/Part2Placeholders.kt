package com.payslipmax.pdfparser.guide.data

/** A reference to a slot: `slot` or, for an order reference, `slot.no` / `slot.date`. */
internal data class SlotRef(
    val slot: String,
    val part: String?,
)

/**
 * The one grammar of Part II template placeholders: `{slot}`, `{slot.no}`, `{slot.date}`. The template validator and the narrative
 * renderer both read it here, so a text the validator accepts is a text the renderer fills the same way. Plain scans, no regex, so it
 * stays linear on Kotlin/Native.
 */
internal object Part2Placeholders {
    const val NO = "no"
    const val DATE = "date"

    /** Every `{...}` in [text] as the text between the braces, and whether the whole text is well formed (closed, not nested, not empty). */
    fun scan(text: String): Pair<List<String>, Boolean> {
        val found = mutableListOf<String>()
        var wellFormed = true
        var at = 0
        while (at < text.length) {
            when (text[at]) {
                '}' -> {
                    wellFormed = false
                    at++
                }
                '{' -> {
                    val end = text.indexOf('}', at + 1)
                    val inner = if (end > 0) text.substring(at + 1, end) else ""
                    if (end < 0 || inner.isEmpty() || '{' in inner) return found to false
                    found += inner
                    at = end + 1
                }
                else -> at++
            }
        }
        return found to wellFormed
    }

    /** Reads `slot` or `slot.no` / `slot.date`; null when it is neither (a bad slot id, a second dot, an unknown part). */
    fun parse(ref: String): SlotRef? {
        val dot = ref.indexOf('.')
        val slot = if (dot < 0) ref else ref.substring(0, dot)
        val part = if (dot < 0) null else ref.substring(dot + 1)
        val partOk = part == null || part == NO || part == DATE
        return if (isGuideId(slot) && partOk) SlotRef(slot, part) else null
    }
}
