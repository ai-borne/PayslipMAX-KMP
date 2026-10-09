package com.payslipmax.pdfparser.guide.domain

/**
 * What a request to set a card's note amounts to. The single place the "blank text means delete" rule lives, so the Room
 * repository, the test fake and any future caller cannot disagree: a valid note is saved, blank text for a valid card id
 * erases that card's note, and anything else (an id or revision that is not a plain token) changes nothing.
 */
sealed interface GuideNoteEdit {
    class Save(val note: GuideNote) : GuideNoteEdit

    class Erase(val cardId: String) : GuideNoteEdit

    data object Reject : GuideNoteEdit

    companion object {
        fun of(
            cardId: String,
            text: String,
            cardRev: String,
            now: Long,
        ): GuideNoteEdit {
            if (!GuidePins.isPlainId(cardId)) return Reject
            GuideNote.of(cardId, text, cardRev, now)?.let { return Save(it) }
            return if (text.isBlank()) Erase(cardId) else Reject
        }
    }
}
