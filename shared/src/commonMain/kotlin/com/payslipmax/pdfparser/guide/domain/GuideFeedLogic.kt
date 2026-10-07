package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.guide.model.GuideCase

/** One card in a case's feed. [alsoHome] is the case the card is homed in when it is an "also relevant here" link. */
data class GuideFeedItem(
    val card: GuideCard,
    val alsoHome: GuideCase? = null,
)

/** Pure rules for a case's feed: which cards, in what order, and when facet chips help (the approved preview). */
object GuideFeedLogic {
    /** Facet chips show only in a feed with more cards than this, and more than one facet. */
    const val FACET_CHIPS_ABOVE_CARDS = 7

    /** The cards homed in [caseId], then its "also relevant here" links, each in bundle order. Empty if unknown. */
    fun feed(
        index: GuideIndex,
        caseId: String,
    ): List<GuideFeedItem> {
        val case = index.case(caseId) ?: return emptyList()
        val homed = case.cards.mapNotNull(index::card).map { GuideFeedItem(it) }
        val also = case.also.mapNotNull(index::card).map { GuideFeedItem(it, alsoHome = index.case(it.nav)) }
        return homed + also
    }

    fun showsFacetChips(items: List<GuideFeedItem>): Boolean =
        items.size > FACET_CHIPS_ABOVE_CARDS && items.distinctBy { it.card.facet }.size > 1

    /** The facets present in [items], in the bundle's facet order ([facetOrder] is the bundle's `facets` keys). */
    fun facetsOf(
        items: List<GuideFeedItem>,
        facetOrder: Collection<String>,
    ): List<String> = facetOrder.filter { facet -> items.any { it.card.facet == facet } }

    /** Filters by [facet] (null means every card); never reorders. */
    fun applyFacet(
        items: List<GuideFeedItem>,
        facet: String?,
    ): List<GuideFeedItem> = if (facet == null) items else items.filter { it.card.facet == facet }

    /**
     * The facet that is actually applied: null (every card) when chips are not shown or no card has [facet], so a
     * restored or stale facet can never leave an empty feed with no chip to clear it.
     */
    fun effectiveFacet(
        items: List<GuideFeedItem>,
        facet: String?,
    ): String? = facet?.takeIf { wanted -> showsFacetChips(items) && items.any { it.card.facet == wanted } }
}
