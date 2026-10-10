package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.GuideNotePlacement
import com.payslipmax.pdfparser.guide.domain.GuideNotePlacements
import com.payslipmax.pdfparser.guide.domain.GuideSearchNotes

/** The small marker a row carries when the user has a note on its card (M7). Search adds [MATCHED_IN_NOTE]. */
enum class GuideNoteMark { NONE, HAS_NOTE, MATCHED_IN_NOTE }

/**
 * One note as a card screen shows it. [sourceCardId] is the row it is stored under: the card itself for the card's own note,
 * an earlier rule's id for a carried one. [editable] is false for a carried note on a card that already has its own note, so
 * moving it can never overwrite that one. The text is private: [toString] prints a length only.
 */
class GuideNoteItem(
    val sourceCardId: String,
    val text: String,
    val stale: Boolean,
    val editable: Boolean,
) {
    override fun toString(): String = "GuideNoteItem(${text.length})"
}

/** The notes a card screen shows: the card's own first, then notes carried from earlier versions of the rule. */
class GuideCardNotes(
    val own: GuideNoteItem?,
    val carried: List<GuideNoteItem>,
) {
    override fun toString(): String = "GuideCardNotes(${carried.size + if (own == null) 0 else 1})"

    companion object {
        val None = GuideCardNotes(null, emptyList())
    }
}

/** A note whose card is no longer in the Guide, for the "Notes on removed cards" list. */
class GuideRemovedNote(
    val cardId: String,
    val text: String,
) {
    override fun toString(): String = "GuideRemovedNote(${text.length})"
}

/**
 * Every readable note placed against the loaded Guide, plus how many rows could not be read. Held in memory by
 * [GuideNotesViewModel] and never saved, logged or sent. A screen asks it only for what it draws, and a locked card asks for nothing.
 */
class GuideNotesState(
    private val placements: GuideNotePlacements,
    val unreadable: Int,
) {
    /** The cards that show a note, own or carried, for the row marker. */
    val notedCards: Set<String> = placements.all.mapNotNull { it.shownOn }.toSet()

    /** Newest first, as [GuideNotePlacements] orders them. */
    val removed: List<GuideRemovedNote> = placements.onRemovedCards.map { GuideRemovedNote(it.note.cardId, it.note.text) }

    fun forCard(cardId: String): GuideCardNotes {
        val placed = placements.forCard(cardId)
        if (placed.isEmpty()) return GuideCardNotes.None
        val ownPlaced = placed.firstOrNull { it.placement == GuideNotePlacement.OWN }
        val own = ownPlaced?.let { GuideNoteItem(it.note.cardId, it.note.text, it.stale, editable = true) }
        val carried =
            placed
                .filter { it.placement == GuideNotePlacement.CARRIED }
                .map { GuideNoteItem(it.note.cardId, it.note.text, stale = true, editable = own == null) }
        return GuideCardNotes(own, carried)
    }

    /** The words of the notes shown on each card, built when search first asks (an unlocked user's search only). */
    val searchNotes: GuideSearchNotes by lazy {
        GuideSearchNotes.of(
            placements.all
                .filter { it.shownOn != null }
                .groupBy { it.shownOn.orEmpty() }
                .mapValues { (_, placed) -> placed.joinToString("\n") { it.note.text } },
        )
    }

    override fun toString(): String = "GuideNotesState(${placements.all.size}, unreadable=$unreadable)"

    companion object {
        val None = GuideNotesState(GuideNotePlacements.of(emptyList(), emptyList()), unreadable = 0)
    }
}
