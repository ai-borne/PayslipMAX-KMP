package com.payslipmax.pdfparser.guide.model

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Each trust chip has exactly one source in the data, so the chip a user sees can never contradict the card:
 * "No official source" is the cite being empty (owner rule, 2026-10-07), not the GUIDANCE tag.
 */
class GuideCardTest {
    private val cards =
        (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle.cards.associateBy { it.id }

    @Test
    fun noOfficialSourceFollowsTheEmptyCite() {
        assertTrue(cards.getValue(SyntheticGuideBundle.NO_CITE_CARD).hasNoOfficialSource)
        assertFalse(cards.getValue(SyntheticGuideBundle.AMENDED_CARD).hasNoOfficialSource)
        // A card that still carried the GUIDANCE tag but cites an authority shows its cite, not the chip.
        assertFalse(cards.getValue(SyntheticGuideBundle.AMENDED_CARD).copy(chips = listOf("GUIDANCE")).hasNoOfficialSource)
    }

    @Test
    fun knownChipsAreTypedAndUnknownOnesAreSetAside() {
        assertEquals(GuideChip.RATES, GuideChip.fromKey("RATES"))
        assertEquals(GuideChip.AMENDED, GuideChip.fromKey("AMENDED"))
        assertEquals(GuideChip.UNKNOWN, GuideChip.fromKey("rates"))
        assertEquals(GuideChip.UNKNOWN, GuideChip.fromKey("UNKNOWN"))
    }

    @Test
    fun unverifiedComesFromTheBundleFlag() {
        assertTrue(cards.getValue(SyntheticGuideBundle.UNVERIFIED_CARD).unverified)
        assertFalse(cards.getValue(SyntheticGuideBundle.PERSONAL_CARD).unverified)
    }
}
