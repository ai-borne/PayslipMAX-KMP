package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A rule can be replaced by a later dated rule, or a card can leave the Guide in an app update. The user's note must
 * neither vanish nor sit on a card the feeds hide: it follows the rule to the end of its `replaced_by` chain (marked stale,
 * because it was written against the old words), and a note with no successor is kept and listed apart.
 */
class GuideNotePlacementsTest {
    private val old = SyntheticGuideRuleChange.OLD_CARD
    private val current = SyntheticGuideRuleChange.NEW_CARD
    private val cards = SyntheticGuideRuleChange.bundle().cards.map { if (it.id == current) it.copy(rev = "newrev01") else it }

    private fun note(
        cardId: String,
        rev: String = "oldrev01",
        at: Long = 1L,
    ) = assertNotNull(GuideNote.of(cardId, "text of $cardId", rev, at))

    private fun chain(vararg replacedBy: Pair<String, String>): List<GuideCard> {
        val template = cards.first { it.id == "RB-P1" }
        val ids = replacedBy.flatMap { listOf(it.first, it.second) }.distinct()
        return ids.map { id -> template.copy(id = id, rev = "rev-$id", replacedBy = replacedBy.firstOrNull { it.first == id }?.second.orEmpty()) }
    }

    @Test
    fun aNoteOnACurrentCardStaysOnItAndIsFreshWhileTheRevisionMatches() {
        val placed = GuideNotePlacements.of(listOf(note(current, rev = "newrev01")), cards).forCard(current).single()

        assertEquals(GuideNotePlacement.OWN, placed.placement)
        assertFalse(placed.stale)
    }

    @Test
    fun aNoteOnACurrentCardGoesStaleWhenTheCardTextChangedAfterTheNoteWasWritten() {
        val placed = GuideNotePlacements.of(listOf(note(current, rev = "oldrev01")), cards).forCard(current).single()

        assertEquals(GuideNotePlacement.OWN, placed.placement)
        assertTrue(placed.stale, "the card was updated since the note")
    }

    @Test
    fun aNoteOnAReplacedCardMovesToTheRuleInForceAndIsAlwaysStale() {
        val placements = GuideNotePlacements.of(listOf(note(old, rev = "whatever")), cards)

        val placed = placements.forCard(current).single()
        assertEquals(GuideNotePlacement.CARRIED, placed.placement)
        assertTrue(placed.stale, "written against the earlier rule, even if the revisions happened to match")
        assertEquals(old, placed.note.cardId, "the note itself still belongs to the card it was written on")
        assertTrue(placements.forCard(old).isEmpty(), "a replaced card is hidden from feeds, so the note must not sit there")
        assertTrue(placements.onRemovedCards.isEmpty())
    }

    @Test
    fun aNoteFollowsTheWholeChainNotJustTheFirstStep() {
        val three = chain("A" to "B", "B" to "C")

        val placements = GuideNotePlacements.of(listOf(note("A")), three)

        assertEquals(listOf("A"), placements.forCard("C").map { it.note.cardId })
        assertTrue(placements.forCard("B").isEmpty())
    }

    @Test
    fun aNoteOnACardThatLeftTheGuideIsKeptAndListedAsOnARemovedCard() {
        val placements = GuideNotePlacements.of(listOf(note("RB-GONE")), cards)

        val removed = placements.onRemovedCards.single()
        assertEquals(GuideNotePlacement.REMOVED, removed.placement)
        assertEquals("RB-GONE", removed.note.cardId)
    }

    @Test
    fun aChainThatLoopsEndsTheWalkAndTheNoteIsListedAsOnARemovedCard() {
        // compile.py ships no loop, but a hand-edited bundle must end the walk, not hang the screen.
        val loop = chain("A" to "B", "B" to "A")

        val placements = GuideNotePlacements.of(listOf(note("A")), loop)

        assertEquals(listOf("A"), placements.onRemovedCards.map { it.note.cardId })
        assertTrue(placements.forCard("A").isEmpty() && placements.forCard("B").isEmpty())
    }

    @Test
    fun aChainThatEndsAtAMissingCardCountsAsRemoved() {
        val dangling = chain("A" to "B").filter { it.id == "A" }

        val placements = GuideNotePlacements.of(listOf(note("A")), dangling)

        assertEquals(listOf("A"), placements.onRemovedCards.map { it.note.cardId })
    }

    @Test
    fun aCardShowsItsOwnNoteBeforeOneCarriedOverAndNewestCarriedFirst() {
        val own = note(current, rev = "newrev01", at = 5L)
        val carried = note(old, at = 99L)

        val onCard = GuideNotePlacements.of(listOf(carried, own), cards).forCard(current)

        assertEquals(listOf(GuideNotePlacement.OWN, GuideNotePlacement.CARRIED), onCard.map { it.placement })
    }

    @Test
    fun removedNotesListNewestFirstAndNothingIsLost() {
        val notes = listOf(note("RB-GONE-1", at = 1L), note("RB-GONE-2", at = 3L), note("RB-GONE-3", at = 2L), note(current, rev = "newrev01"))

        val placements = GuideNotePlacements.of(notes, cards)

        assertEquals(listOf("RB-GONE-2", "RB-GONE-3", "RB-GONE-1"), placements.onRemovedCards.map { it.note.cardId })
        assertEquals(4, placements.all.size, "every note lands in exactly one place")
    }

    @Test
    fun noNotesPlaceNowhere() {
        val placements = GuideNotePlacements.of(emptyList(), cards)

        assertTrue(placements.all.isEmpty() && placements.onRemovedCards.isEmpty() && placements.forCard(current).isEmpty())
    }
}
