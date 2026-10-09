package com.payslipmax.pdfparser.guide.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A note is the user's private text about one card. The rules below live in the domain, not only in the editor, so a
 * damaged or tampered stored value, a restored backup or a future caller cannot put an oversized or mis-keyed note on screen.
 */
class GuideNoteTest {
    private fun note(
        cardId: String = "RB-TD-001",
        text: String = "ask the unit clerk",
        rev: String = "a1b2c3d4",
        at: Long = 10L,
    ) = GuideNote.of(cardId, text, rev, at)

    @Test
    fun aNoteKeepsItsTrimmedTextAndTheCardRevisionItWasWrittenAgainst() {
        val made = assertNotNull(note(text = "  ask the unit clerk \n"))

        assertEquals("ask the unit clerk", made.text)
        assertEquals("RB-TD-001", made.cardId)
        assertEquals("a1b2c3d4", made.cardRev)
        assertEquals(10L, made.updatedAt)
    }

    @Test
    fun blankTextMeansNoNoteAtAll() {
        assertNull(note(text = ""))
        assertNull(note(text = "   \n\t "), "whitespace is not a note; the editor treats it as delete")
    }

    @Test
    fun aCardIdThatIsNotAPlainIdIsRefused() {
        // The id is the database key and later reaches screens; anything that could carry markup or a path is refused.
        listOf("", "RB TD", "<b>", "../x", "RB-TD-001\n", "x".repeat(65)).forEach {
            assertNull(note(cardId = it), "id '$it' must be refused")
        }
        assertNotNull(note(cardId = "x".repeat(64)))
    }

    @Test
    fun aRevisionThatIsNotAPlainTokenIsRefusedButAnEmptyOneIsAllowed() {
        assertNull(note(rev = "<script>"))
        assertNotNull(note(rev = ""), "a bundle written before revisions existed has none")
    }

    @Test
    fun textIsCappedAtTheLimitInTheDomainNotJustTheEditor() {
        val exactly = "a".repeat(GUIDE_NOTE_MAX_CHARS)

        assertEquals(GUIDE_NOTE_MAX_CHARS, assertNotNull(note(text = exactly)).text.length)
        assertEquals(GUIDE_NOTE_MAX_CHARS, assertNotNull(note(text = exactly + "b".repeat(500))).text.length)
        assertEquals(2000, GUIDE_NOTE_MAX_CHARS, "the owner-approved limit")
    }

    @Test
    fun aCapNeverSplitsAnEmojiInHalf() {
        // U+1F600 is two chars (a surrogate pair). A cut between them would store a broken character.
        val text = "a".repeat(GUIDE_NOTE_MAX_CHARS - 1) + "😀"

        val kept = assertNotNull(note(text = text)).text

        assertEquals("a".repeat(GUIDE_NOTE_MAX_CHARS - 1), kept)
        assertFalse(kept.last().isHighSurrogate())
    }

    @Test
    fun aCapThatLeavesTrailingSpacesTrimsThemToo() {
        val text = "a".repeat(GUIDE_NOTE_MAX_CHARS - 3) + "   " + "tail"

        val kept = assertNotNull(note(text = text)).text

        assertEquals("a".repeat(GUIDE_NOTE_MAX_CHARS - 3), kept)
    }

    @Test
    fun aNoteIsStaleOnlyWhenTheCardsRevisionChangedSinceItWasWritten() {
        val made = assertNotNull(note(rev = "a1b2c3d4"))

        assertFalse(made.isStale("a1b2c3d4"))
        assertTrue(made.isStale("ffffffff"))
    }

    @Test
    fun printingANoteNeverShowsItsText() {
        // A note reaches no log on purpose; toString is the usual leak (string templates, assertion messages, crash keys).
        val made = assertNotNull(note(text = "my private secret"))

        assertFalse(made.toString().contains("private"))
        assertFalse(made.toString().contains("RB-TD-001"))
    }

    @Test
    fun twoNotesWithTheSameFieldsAreEqualAndItsHashMatches() {
        assertEquals(note(), note())
        assertEquals(note().hashCode(), note().hashCode())
        assertTrue(note(text = "one") != note(text = "two"))
    }
}
