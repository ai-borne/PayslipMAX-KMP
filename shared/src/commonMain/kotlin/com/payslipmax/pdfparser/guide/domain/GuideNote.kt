package com.payslipmax.pdfparser.guide.domain

/** The most characters a note keeps (owner decision, 2026-10-09). Enforced here so no caller or restored file can exceed it. */
const val GUIDE_NOTE_MAX_CHARS = 2000

/**
 * A private note the user wrote about one Guide card: never part of the official text, never shared, never sent anywhere.
 * [cardRev] is the card's `rev` when the note was written, so [isStale] can tell the user the card changed since.
 *
 * Immutable and built only through [of], which is the one place the rules live: the card id must be a plain id (it is the
 * database key), the text is trimmed and capped at [GUIDE_NOTE_MAX_CHARS], and blank text is not a note. Not a data class:
 * its generated `copy()` would let a caller skip those rules, and its generated `toString()` would print the text.
 */
class GuideNote private constructor(
    val cardId: String,
    val text: String,
    val cardRev: String,
    val updatedAt: Long,
) {
    /** True when the card's revision is no longer the one this note was written against. */
    fun isStale(currentRev: String): Boolean = cardRev != currentRev

    override fun equals(other: Any?): Boolean =
        other is GuideNote && other.cardId == cardId && other.text == text && other.cardRev == cardRev && other.updatedAt == updatedAt

    override fun hashCode(): Int = ((cardId.hashCode() * HASH_PRIME + text.hashCode()) * HASH_PRIME + cardRev.hashCode()) * HASH_PRIME + updatedAt.hashCode()

    // The text is private: a string template, an assertion message or a crash key must never be able to print it.
    override fun toString(): String = "GuideNote(${text.length})"

    companion object {
        private const val HASH_PRIME = 31

        /** The note, or null when [cardId] or [cardRev] is not a plain token or [text] is blank (which means "no note"). */
        fun of(
            cardId: String,
            text: String,
            cardRev: String,
            updatedAt: Long,
        ): GuideNote? {
            if (!GuidePins.isPlainId(cardId)) return null
            if (cardRev.isNotEmpty() && !GuidePins.isPlainId(cardRev)) return null
            val kept = capped(text.trim())
            return if (kept.isEmpty()) null else GuideNote(cardId, kept, cardRev, updatedAt)
        }

        /** Cuts to the limit without leaving half an emoji (a lone high surrogate) or trailing spaces at the cut. */
        private fun capped(text: String): String {
            if (text.length <= GUIDE_NOTE_MAX_CHARS) return text
            var end = GUIDE_NOTE_MAX_CHARS
            if (text[end - 1].isHighSurrogate()) end--
            return text.substring(0, end).trimEnd()
        }
    }
}
