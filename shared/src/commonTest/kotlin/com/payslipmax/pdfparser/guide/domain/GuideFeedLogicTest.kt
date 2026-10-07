package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The feed is the screen a user scrolls to find their situation. Facet chips help only in a long feed with more
 * than one kind of card (the approved preview: small topics skip them), filtering must never reorder the cards
 * the authors ordered, and an "also relevant here" card must say where it really lives.
 */
class GuideFeedLogicTest {
    private val bundle: GuideBundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON)).bundle

    private fun feed(
        caseId: String,
        source: GuideBundle = bundle,
    ) = GuideFeedLogic.feed(GuideIndex(source), caseId)

    /** The big case trimmed to its first [count] cards, optionally all set to one facet. */
    private fun bigCase(
        count: Int,
        oneFacet: String? = null,
    ): GuideBundle {
        val kept = bundle.nav[0].cases[0].cards.take(count)
        val nav = bundle.nav.map { area -> area.copy(cases = area.cases.map { if (it.id == SyntheticGuideBundle.BIG_CASE) it.copy(cards = kept) else it }) }
        val cards = bundle.cards.map { if (oneFacet != null && it.id in kept) it.copy(facet = oneFacet) else it }
        return bundle.copy(nav = nav, cards = cards)
    }

    @Test
    fun aFeedListsHomedCardsThenAlsoLinksInBundleOrder() {
        val items = feed("ltc-home")

        assertEquals(listOf("RB-T9", "RB-T10", "RB-T1"), items.map { it.card.id })
        assertNull(items[0].alsoHome, "a card homed here is not an also-link")
        assertEquals(SyntheticGuideBundle.BIG_CASE, items[2].alsoHome?.id, "the also-link names its main home")
    }

    @Test
    fun anUnknownCaseHasAnEmptyFeed() {
        assertTrue(feed("gone").isEmpty())
    }

    @Test
    fun facetChipsShowAtEightCardsButNotAtSeven() {
        assertTrue(GuideFeedLogic.showsFacetChips(feed(SyntheticGuideBundle.BIG_CASE, bigCase(8))))
        assertFalse(GuideFeedLogic.showsFacetChips(feed(SyntheticGuideBundle.BIG_CASE, bigCase(7))))
    }

    @Test
    fun facetChipsNeedMoreThanOneFacet() {
        assertFalse(GuideFeedLogic.showsFacetChips(feed(SyntheticGuideBundle.BIG_CASE, bigCase(8, oneFacet = "H"))))
    }

    @Test
    fun facetsFollowTheBundleOrderAndOnlyThoseInTheFeed() {
        // Bundle order is Q, H, C, L; the big case has all four.
        assertEquals(listOf("Q", "H", "C", "L"), GuideFeedLogic.facetsOf(feed(SyntheticGuideBundle.BIG_CASE), bundle.facets.keys))
        // pay-hra has Q, H and L only.
        assertEquals(listOf("Q", "H", "L"), GuideFeedLogic.facetsOf(feed("pay-hra"), bundle.facets.keys))
    }

    @Test
    fun filteringByAFacetKeepsTheAuthoredOrder() {
        val items = feed(SyntheticGuideBundle.BIG_CASE)

        val howMuch = GuideFeedLogic.applyFacet(items, "H")

        assertEquals(listOf("RB-T1", "RB-T2", "RB-T7"), howMuch.map { it.card.id })
        assertEquals(items, GuideFeedLogic.applyFacet(items, null), "no facet means every card")
    }

    @Test
    fun aFacetIsOnlyAppliedWhereItsChipCouldBeShown() {
        val big = feed(SyntheticGuideBundle.BIG_CASE)
        assertEquals("H", GuideFeedLogic.effectiveFacet(big, "H"))
        // A restored facet on a small feed (no chips) or one this case has no card for would hide every card.
        assertNull(GuideFeedLogic.effectiveFacet(feed("ltc-home"), "Q"))
        assertNull(GuideFeedLogic.effectiveFacet(feed(SyntheticGuideBundle.BIG_CASE, bigCase(8, oneFacet = "H")), "Q"))
        assertNull(GuideFeedLogic.effectiveFacet(big, null))
    }
}
