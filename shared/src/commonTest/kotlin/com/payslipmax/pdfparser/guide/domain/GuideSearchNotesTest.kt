package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * M7: guide search also looks in the user's own notes. WHY each rule matters: the notes are private and decrypted only in
 * memory, so the index must stay a pure function of what it is handed (nothing remembered between calls); a free (preview)
 * search must not learn anything from a note; and a hit that exists only because of a note must say so, so the user is not
 * left wondering why a card matched.
 */
class GuideSearchNotesTest {
    private val bundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON)).bundle
    private val template = bundle.cards.first { it.id == "RB-P1" }

    private fun card(
        id: String,
        title: String = "Plain title $id",
        details: String = "Plain details.",
    ): GuideCard = template.copy(id = id, title = title, details = details, cite = "", answer = "Plain answer.", key = listOf("A plain key point"))

    private fun indexOf(vararg cards: GuideCard) = GuideSearchIndex(bundle.copy(cards = cards.toList()))

    private fun notes(vararg pairs: Pair<String, String>) = GuideSearchNotes.of(mapOf(*pairs))

    @Test
    fun aWordThatIsOnlyInTheNoteFindsTheCardAndSaysSo() {
        val index = indexOf(card("a"), card("b"))

        val hits = index.search("zebra", notes = notes("a" to "Ask the unit about the zebra form"))

        assertEquals(listOf("a"), hits.map { it.card.id })
        assertTrue(hits.single().matchedInNote)
    }

    @Test
    fun withoutNotesNothingMatchesAndNothingIsRemembered() {
        val index = indexOf(card("a"))

        assertEquals(1, index.search("zebra", notes = notes("a" to "zebra")).size)
        // The index kept nothing from the call above: it is a pure function of its inputs.
        assertEquals(emptyList(), index.search("zebra"))
        assertEquals(emptyList(), index.search("zebra", notes = GuideSearchNotes.None))
    }

    @Test
    fun aCardThatMatchesOnItsOwnWordsIsNotFlaggedEvenWhenItsNoteMatchesToo() {
        val index = indexOf(card("a", title = "Zebra allowance"))

        val hit = index.search("zebra", notes = notes("a" to "my zebra reminder")).single()

        assertFalse(hit.matchedInNote, "the card matched without the note, so the marker would be misleading")
    }

    @Test
    fun oneQueryWordInTheCardAndAnotherInTheNoteStillMatchesAndIsFlagged() {
        val index = indexOf(card("a", title = "Zebra allowance"), card("b", title = "Zebra other"))

        val hits = index.search("zebra blue", notes = notes("a" to "the form is blue"))

        assertEquals(listOf("a"), hits.map { it.card.id })
        assertTrue(hits.single().matchedInNote)
    }

    @Test
    fun theFreePreviewIgnoresNotesSoItCannotLearnFromThem() {
        val index = indexOf(card("a", title = "Zebra allowance"))

        val noteOnly = index.search("blue", GuideSearchScope.PREVIEW, notes("a" to "the form is blue"))
        val titled = index.search("zebra", GuideSearchScope.PREVIEW, notes("a" to "the form is blue"))

        assertEquals(emptyList(), noteOnly)
        assertFalse(titled.single().matchedInNote)
    }

    @Test
    fun aNoteWordRanksBelowEveryFieldOfTheCardItself() {
        val index = indexOf(card("inDetails", details = "mention zebra here"), card("inNote"))

        val hits = index.search("zebra", notes = notes("inNote" to "zebra"))

        assertEquals(listOf("inDetails", "inNote"), hits.map { it.card.id })
        assertTrue(GuideSearchRanking.NOTE < GuideSearchRanking.DETAILS)
    }

    @Test
    fun aNumberInANoteMatchesAsAWholeNumberLikeInTheCardText() {
        val index = indexOf(card("a"), card("b"))

        val hits = index.search("114", notes = notes("a" to "see order 114 on the file", "b" to "see order 1140 on the file"))

        assertEquals(listOf("a"), hits.map { it.card.id })
    }

    @Test
    fun aNoteForAReplacedCardIsNotSearchedBecauseTheCardIsNot() {
        val index = GuideSearchIndex(SyntheticGuideRuleChange.bundle())

        val hits = index.search("zebra", notes = notes(SyntheticGuideRuleChange.OLD_CARD to "zebra"))

        assertEquals(emptyList(), hits)
    }

    @Test
    fun theSearchNotesHoldWordsOnlyAndPrintOnlyACount() {
        val text = notes("a" to "Secret zebra words", "b" to "  ")

        assertEquals("GuideSearchNotes(1)", text.toString(), "a blank note is not searchable and the text never prints")
        assertEquals(setOf("secret", "zebra", "words"), text.words("a"))
        assertEquals(emptySet(), text.words("b"))
    }
}
