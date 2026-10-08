package com.payslipmax.pdfparser.guide.domain

private const val MAX_ID_LENGTH = 64
private const val ID_EXTRA_CHARS = "._-"
private const val STORED_SEPARATOR = '\n'

/**
 * The cards a user pinned, oldest first (the order they were pinned in). Immutable: every change returns a new value.
 * Holds plain card ids only, never card text or anything about the user. [fromStored] keeps only ids that look like
 * card ids, so a damaged or tampered stored value cannot put anything else on screen.
 */
class GuidePins private constructor(private val oldestFirst: List<String>) {
    /** The order Guide Home shows: the card pinned last is first. */
    val newestFirst: List<String> get() = oldestFirst.asReversed()

    fun isPinned(cardId: String): Boolean = cardId in oldestFirst

    /** Unpins a pinned card; otherwise pins it as the newest. An id that is not a plain card id is ignored. */
    fun toggle(cardId: String): GuidePins =
        when {
            isPinned(cardId) -> GuidePins(oldestFirst - cardId)
            isPlainId(cardId) -> GuidePins(oldestFirst + cardId)
            else -> this
        }

    /** Drops ids the loaded bundle no longer holds (a card removed by an app update). */
    fun retainKnown(isKnown: (String) -> Boolean): GuidePins = GuidePins(oldestFirst.filter(isKnown))

    fun toStored(): String = oldestFirst.joinToString(STORED_SEPARATOR.toString())

    // Not a data class: its generated copy() would let a caller build an instance that skipped the id check.
    override fun equals(other: Any?): Boolean = other is GuidePins && other.oldestFirst == oldestFirst

    override fun hashCode(): Int = oldestFirst.hashCode()

    override fun toString(): String = "GuidePins(${oldestFirst.size})"

    companion object {
        val Empty = GuidePins(emptyList())

        fun fromStored(raw: String?): GuidePins =
            GuidePins(raw.orEmpty().split(STORED_SEPARATOR).filter(::isPlainId).distinct())

        /** ASCII letters, digits and `._-`, one to 64 characters: every real card id, and nothing that could carry markup. */
        internal fun isPlainId(id: String): Boolean = id.length in 1..MAX_ID_LENGTH && id.all(::isIdChar)

        private fun isIdChar(c: Char): Boolean = c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c in ID_EXTRA_CHARS
    }
}
