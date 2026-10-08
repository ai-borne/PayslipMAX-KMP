package com.payslipmax.pdfparser.guide.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GuideShareTextTest {
    private val labels =
        GuideShareLabels(
            header = "Claim note",
            answer = "Answer:",
            keyPoints = "Key points:",
            attach = "Attach:",
            watchOut = "Watch out:",
            authority = "Authority:",
            unverified = "Unverified point: still being checked.",
        )

    private val parts =
        GuideShareParts(
            title = "What is the food rate?",
            answer = "Rs 900 a day at level 11.",
            key = listOf("Full day only", "Claim in the TA bill"),
            attach = listOf("Movement order"),
            watch = listOf("Meals provided by the unit"),
            cite = "MoD letter 1/2/3",
            unverified = false,
        )

    @Test
    fun theNoteIsExactlyThisTextForAFullCard() {
        val expected =
            """
            Claim note

            What is the food rate?

            Answer: Rs 900 a day at level 11.

            Key points:
            - Full day only
            - Claim in the TA bill

            Attach:
            - Movement order

            Watch out:
            - Meals provided by the unit

            Authority: MoD letter 1/2/3
            """.trimIndent()

        assertEquals(expected, GuideShareText.build(parts, labels))
    }

    @Test
    fun anUnverifiedCardCarriesTheWarningDirectlyUnderTheAnswer() {
        val note = GuideShareText.build(parts.copy(unverified = true), labels)

        assertEquals(
            "Claim note\n\nWhat is the food rate?\n\nAnswer: Rs 900 a day at level 11.\nUnverified point: still being checked.\n\nKey points:",
            note.substringBefore("\n- Full day only"),
        )
    }

    @Test
    fun aCardWithoutAttachWatchOutOrCiteLeavesThoseSectionsOut() {
        val note = GuideShareText.build(parts.copy(attach = emptyList(), watch = emptyList(), cite = ""), labels)

        assertEquals(
            "Claim note\n\nWhat is the food rate?\n\nAnswer: Rs 900 a day at level 11.\n\nKey points:\n- Full day only\n- Claim in the TA bill",
            note,
        )
    }

    @Test
    fun aVerifiedCardNeverMentionsTheWarning() {
        assertFalse(GuideShareText.build(parts, labels).contains(labels.unverified))
    }
}
