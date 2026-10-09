package com.payslipmax.pdfparser.guide.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * M5: the email a user may send to suggest a correction. The body is built from an allow-list, so what the app adds is exactly
 * the card id and title, the bundle date, the card revision and the app version; the user's text is the only free part.
 */
class GuideSuggestionTest {
    private val labels =
        GuideSuggestionLabels(
            subjectTag = "[Guide]",
            header = "Guide correction suggestion",
            card = "Card:",
            title = "Title:",
            bundle = "Guide data:",
            revision = "Card revision:",
            app = "App version:",
            suggestion = "Suggestion:",
        )

    private fun parts(
        text: String = "The rate changed.",
        rev: String = "ab12cd34",
        generated: String = "2026-10-07",
    ) = GuideSuggestionParts("RB-T068", "What is the food rate?", generated, rev, "1.3.0", text)

    @Test
    fun theSubjectIsTheTagAndTheCardIdAndNothingElse() {
        assertEquals("[Guide] RB-T068", GuideSuggestion.subject(parts(), labels))
    }

    @Test
    fun theBodyIsExactlyThisTextAndHoldsOnlyAllowListedFields() {
        val expected =
            """
            Guide correction suggestion

            Card: RB-T068
            Title: What is the food rate?
            Guide data: 2026-10-07
            Card revision: ab12cd34
            App version: 1.3.0

            Suggestion:
            The rate changed.
            """.trimIndent()

        assertEquals(expected, GuideSuggestion.body(parts(), labels))
    }

    @Test
    fun aBlankRevisionOrBundleDateLeavesItsLineOutInsteadOfPrintingAnEmptyValue() {
        val body = GuideSuggestion.body(parts(rev = "", generated = " "), labels)

        assertFalse(body.contains("Card revision:"))
        assertFalse(body.contains("Guide data:"))
        assertTrue(body.contains("App version: 1.3.0"))
    }

    @Test
    fun theTextIsTrimmedButInnerLineBreaksAndSpacesStay() {
        val body = GuideSuggestion.body(parts(text = "  \n line one\n\n  line two \t\n"), labels)

        assertTrue(body.endsWith("Suggestion:\nline one\n\n  line two"))
    }

    @Test
    fun theTextIsCappedAtTheLimitAndAnInputAtTheLimitIsKept() {
        val atLimit = "a".repeat(GuideSuggestion.MAX_TEXT_LENGTH)

        assertEquals(atLimit, GuideSuggestion.clean(atLimit))
        assertEquals(atLimit, GuideSuggestion.clean(atLimit + "bbbb"))
        assertEquals(1000, GuideSuggestion.MAX_TEXT_LENGTH, "the same cap as Report an Issue")
    }

    @Test
    fun aCutThatWouldSplitAnEmojiDropsTheHalfInsteadOfSendingABrokenCharacter() {
        val emoji = "😀"
        val input = "a".repeat(GuideSuggestion.MAX_TEXT_LENGTH - 1) + emoji

        val cleaned = GuideSuggestion.clean(input)

        assertEquals("a".repeat(GuideSuggestion.MAX_TEXT_LENGTH - 1), cleaned)
        assertFalse(cleaned.last().isHighSurrogate())
    }

    @Test
    fun anEmojiThatFitsWholeIsKept() {
        val input = "a".repeat(GuideSuggestion.MAX_TEXT_LENGTH - 2) + "😀"

        assertEquals(input, GuideSuggestion.clean(input))
    }

    @Test
    fun specialCharactersAndNonLatinTextPassThroughUnchanged() {
        val text = "Rs 1,500 & \"quotes\" <b>tag</b> 100% = ₹1,500 + #1? रिपोर्ट 😀 [x]"

        assertTrue(GuideSuggestion.body(parts(text = text), labels).endsWith("Suggestion:\n$text"))
    }

    @Test
    fun emptyOrBlankTextCannotBeSent() {
        assertFalse(GuideSuggestion.canSend(""))
        assertFalse(GuideSuggestion.canSend("   \n\t "))
        assertTrue(GuideSuggestion.canSend(" x "))
    }

    @Test
    fun aReplacedCardsIdWorksLikeAnyOther() {
        val replaced = parts().copy(cardId = "RB-OLD-114")

        assertEquals("[Guide] RB-OLD-114", GuideSuggestion.subject(replaced, labels))
        assertTrue(GuideSuggestion.body(replaced, labels).contains("Card: RB-OLD-114\n"))
    }
}
