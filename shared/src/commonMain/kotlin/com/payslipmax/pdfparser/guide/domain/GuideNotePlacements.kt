package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideCard

/** Where a note shows: on the card it was written on, carried to the card that replaced that rule, or on a card that is gone. */
enum class GuideNotePlacement { OWN, CARRIED, REMOVED }

/**
 * One note and where it shows. [shownOn] is the card whose screen shows it, null for [GuideNotePlacement.REMOVED]. A carried
 * note is always [stale]: it was written against an earlier rule. An own note is stale when the card's `rev` moved on.
 */
class PlacedGuideNote(
    val note: GuideNote,
    val placement: GuideNotePlacement,
    val shownOn: String?,
    val stale: Boolean,
)

/**
 * Every note placed against the loaded Guide, owner decisions 2026-10-09: a note on a replaced card follows `replaced_by`
 * to the END of the chain (the rule in force); a note whose card has no successor, is unknown to this bundle or sits on a
 * chain that loops or dangles is kept and listed as "on a removed card". The walk is [GuideRuleHistory]'s, which keeps a
 * visited set, so a damaged bundle cannot hang a screen. Pure: no storage, no text of its own.
 */
class GuideNotePlacements private constructor(val all: List<PlacedGuideNote>) {
    private val byCard: Map<String, List<PlacedGuideNote>> =
        all.filter { it.shownOn != null }.groupBy { it.shownOn.orEmpty() }

    /** Notes shown on [cardId]: its own first, then notes carried from earlier rules, newest first. */
    fun forCard(cardId: String): List<PlacedGuideNote> = byCard[cardId].orEmpty()

    /** Notes on cards that no longer exist, newest first, for the "Notes on removed cards" list. */
    val onRemovedCards: List<PlacedGuideNote> = all.filter { it.placement == GuideNotePlacement.REMOVED }

    companion object {
        fun of(
            notes: List<GuideNote>,
            cards: List<GuideCard>,
        ): GuideNotePlacements {
            val byId = cards.associateBy { it.id }
            val history = GuideRuleHistory(cards)
            val placed = notes.map { place(it, byId, history) }
            val ordered =
                placed.sortedWith(
                    compareBy<PlacedGuideNote> { it.placement.ordinal }.thenByDescending { it.note.updatedAt }.thenBy { it.note.cardId },
                )
            return GuideNotePlacements(ordered)
        }

        private fun place(
            note: GuideNote,
            byId: Map<String, GuideCard>,
            history: GuideRuleHistory,
        ): PlacedGuideNote {
            val card = byId[note.cardId] ?: return removed(note)
            if (!card.isReplaced) {
                return PlacedGuideNote(note, GuideNotePlacement.OWN, card.id, stale = note.isStale(card.rev))
            }
            val inForce = history.currentRule(card.id) ?: return removed(note)
            return PlacedGuideNote(note, GuideNotePlacement.CARRIED, inForce.id, stale = true)
        }

        private fun removed(note: GuideNote) = PlacedGuideNote(note, GuideNotePlacement.REMOVED, shownOn = null, stale = false)
    }
}
