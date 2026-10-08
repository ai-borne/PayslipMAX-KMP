package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.domain.GuideIndex
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuideCardShareTest {
    private val index = GuideIndex((GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle)

    private fun card(
        id: String,
        unlocked: Boolean = true,
    ) = checkNotNull(index.cardContent(id, unlocked, nowMillis = 0L))

    @Test
    fun theClaimNoteIsExactlyThisTextForACardWithACite() {
        val expected =
            """
            Claim note

            Synthetic card RB-T5?

            Answer: A one-line answer for RB-T5.

            Key points:
            - A short key point for RB-T5

            Attach:
            - A form

            Authority: Rule 114 TR
            """.trimIndent()

        assertEquals(expected, card("RB-T5").shareNote())
    }

    @Test
    fun anUnverifiedCardCarriesTheWarningLineButAVerifiedOneDoesNot() {
        val warning = "Unverified point: still being checked. Confirm it against the current order before you rely on it."

        assertTrue(card(SyntheticGuideBundle.UNVERIFIED_CARD).shareNote()!!.contains("\n$warning\n"))
        assertFalse(card("RB-T5").shareNote()!!.contains(warning))
    }

    @Test
    fun aCardWithNoCiteHasNoAuthorityLine() {
        assertFalse(card(SyntheticGuideBundle.NO_CITE_CARD).shareNote()!!.contains("Authority:"))
    }

    @Test
    fun theNoteLeavesOutTheDetailsText() {
        assertFalse(card("RB-T5").shareNote()!!.contains("Longer details"))
    }

    @Test
    fun aLockedCardHasNothingToShare() {
        assertNull(card("RB-T5", unlocked = false).shareNote())
    }
}
