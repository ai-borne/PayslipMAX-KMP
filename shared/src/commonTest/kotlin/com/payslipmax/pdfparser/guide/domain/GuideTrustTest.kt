package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The four trust chips tell a claimant how far to lean on a card (owner decisions 2026-10-06/07). Each has exactly one
 * source, so a card can never show a chip its data does not back: Rates as of = the RATES chip plus the bundle's
 * `rates_as_of`; Amended = the AMENDED chip; Unverified point = `unverified`; No official source = an empty cite.
 */
class GuideTrustTest {
    private val bundle = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle

    private fun trust(id: String) = GuideTrust.of(bundle.cards.first { it.id == id }, bundle.ratesAsOf)

    @Test
    fun aRatesCardCarriesTheBundlesRatesDateAndNothingElseDoes() {
        assertEquals(SyntheticGuideBundle.RATES_AS_OF, trust(SyntheticGuideBundle.PERSONAL_CARD).ratesAsOf)
        assertNull(trust("RB-T5").ratesAsOf, "a card without the RATES chip shows no date")
    }

    @Test
    fun anUnverifiedCardIsFlaggedFromItsUnverifiedFieldOnly() {
        assertTrue(trust(SyntheticGuideBundle.UNVERIFIED_CARD).unverified)
        assertFalse(trust("RB-T5").unverified)
    }

    @Test
    fun noOfficialSourceMeansAnEmptyCiteNotTheGuidanceChipText() {
        assertTrue(trust(SyntheticGuideBundle.NO_CITE_CARD).noOfficialSource)
        val cited = bundle.cards.first { it.id == "RB-T5" }
        assertFalse(GuideTrust.of(cited.copy(chips = listOf("GUIDANCE")), bundle.ratesAsOf).noOfficialSource, "the cite decides")
    }

    @Test
    fun anAmendedCardShowsTheAmendedChip() {
        assertTrue(trust(SyntheticGuideBundle.AMENDED_CARD).amended)
        assertFalse(trust("RB-T5").amended)
    }

    @Test
    fun aPlainCitedCardHasNoChipAtAll() {
        assertFalse(trust("RB-T5").hasAny)
        assertTrue(trust("RB-T2").hasAny)
    }

    @Test
    fun anUnknownChipFromANewerBundleIsIgnored() {
        val card = bundle.cards.first { it.id == "RB-T5" }.copy(chips = listOf("SOMETHING_NEW"))

        assertFalse(GuideTrust.of(card, bundle.ratesAsOf).hasAny)
    }
}
