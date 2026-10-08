package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.domain.CardTemplate
import com.payslipmax.pdfparser.guide.domain.GuideCardBody
import com.payslipmax.pdfparser.guide.domain.GuideFeedLogic
import com.payslipmax.pdfparser.guide.domain.GuideIndex
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.guide.domain.GuideSearchIndex
import com.payslipmax.pdfparser.guide.domain.GuideStaleness
import com.payslipmax.pdfparser.guide.domain.GuideTrust
import com.payslipmax.pdfparser.guide.domain.PersonalFigure
import com.payslipmax.pdfparser.guide.domain.PersonalFigureResolver
import com.payslipmax.pdfparser.guide.model.GuideArea
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideCase

/** The Guide tab's load state. Every screen below Home derives its content from [Ready.bundle]. */
sealed interface GuideUiState {
    data object Loading : GuideUiState

    /** Shown with a retry button, never a blank tab. */
    data class Failed(val error: GuideLoadError) : GuideUiState

    data class Ready(val bundle: GuideBundle, val areas: List<GuideAreaTile>) : GuideUiState {
        /** Id lookups, built once per load. */
        val index: GuideIndex = GuideIndex(bundle)

        /** Built on the first search, not on load, so opening the Guide never pays for it. */
        val searchIndex: GuideSearchIndex by lazy { GuideSearchIndex(bundle) }
    }
}

/** An area tile on Guide Home. Titles are content from the bundle, not UI copy. */
data class GuideAreaTile(val id: String, val title: String, val caseCount: Int)

/** A case tile: [subtitle] is the rule-number line from the bundle (may be empty). */
data class GuideCaseTile(val id: String, val title: String, val subtitle: String, val cardCount: Int)

data class GuideAreaContent(val id: String, val title: String, val cases: List<GuideCaseTile>)

/** A facet chip: the facet key, its label from the bundle, and how many of the feed's cards carry it. */
data class GuideFacetCount(val key: String, val label: String, val count: Int)

/**
 * A card in a feed. [alsoHomeTitle] names the card's main case when it is an "also relevant here" link. [trust] holds
 * only flags and a date, so it is free for everyone; the row has no paid field to hide.
 */
data class GuideFeedRow(
    val cardId: String,
    val title: String,
    val answer: String,
    val facetLabel: String,
    val alsoHomeTitle: String?,
    val trust: GuideTrust,
)

/** A case's feed. [facets] is empty when the feed is too small for chips; a null [selectedFacet] means every card. */
data class GuideFeedContent(
    val caseId: String,
    val title: String,
    val subtitle: String,
    val totalCount: Int,
    val facets: List<GuideFacetCount>,
    val selectedFacet: String?,
    val rows: List<GuideFeedRow>,
)

/**
 * A card as the card screen shows it. The free half (title, one-line answer, trust chips) is always set. The paid half
 * is [full], which is null for a user without access: the key points, cite and details are absent from the state
 * itself, not hidden by the UI, so no screen can draw them by mistake. [ratesStale] asks for the "rates may have
 * changed" nudge.
 */
data class GuideCardContent(
    val id: String,
    val title: String,
    val answer: String,
    val facetLabel: String,
    val trust: GuideTrust,
    val ratesStale: Boolean,
    val full: GuideCardFull?,
)

/**
 * The paid half of a card; [body] holds the placeholder bullets apart, so they are never drawn raw. [figure] is the
 * personal "your figure" line, null when the card has none or the profile cannot settle it. It lives here, not on
 * [GuideCardContent], so a locked card state cannot hold a resolved figure.
 */
data class GuideCardFull(
    val body: GuideCardBody,
    val cite: String,
    val details: String,
    val figure: PersonalFigure? = null,
)

internal fun GuideArea.toTile(): GuideAreaTile = GuideAreaTile(id, title, cases.size)

internal fun GuideArea.toContent(): GuideAreaContent = GuideAreaContent(id, title, cases.map(GuideCase::toTile))

/** The count is every card the case's feed lists: those homed here plus the "also relevant here" links. */
internal fun GuideCase.toTile(): GuideCaseTile = GuideCaseTile(id, title, sub, cards.size + also.size)

internal fun GuideBundle.toReady(): GuideUiState.Ready = GuideUiState.Ready(this, nav.map(GuideArea::toTile))

internal fun GuideIndex.feedContent(
    caseId: String,
    facet: String?,
): GuideFeedContent? {
    val case = case(caseId) ?: return null
    val items = GuideFeedLogic.feed(this, caseId)
    val labels = bundle.facets
    val facets =
        if (!GuideFeedLogic.showsFacetChips(items)) {
            emptyList()
        } else {
            GuideFeedLogic.facetsOf(items, labels.keys).map { key -> GuideFacetCount(key, labels.getValue(key), items.count { it.card.facet == key }) }
        }
    val selected = GuideFeedLogic.effectiveFacet(items, facet)
    val rows =
        GuideFeedLogic.applyFacet(items, selected).map { item ->
            GuideFeedRow(
                item.card.id,
                item.card.title,
                item.card.answer,
                labels[item.card.facet].orEmpty(),
                item.alsoHome?.title,
                GuideTrust.of(item.card, bundle.ratesAsOf),
            )
        }
    return GuideFeedContent(case.id, case.title, case.sub, items.size, facets, selected, rows)
}

internal fun GuideIndex.cardContent(
    cardId: String,
    unlocked: Boolean,
    nowMillis: Long,
    profile: GuideProfile? = null,
): GuideCardContent? {
    val card = card(cardId) ?: return null
    val trust = GuideTrust.of(card, bundle.ratesAsOf)
    val full =
        if (unlocked) {
            GuideCardFull(CardTemplate.body(card), card.cite, card.details, PersonalFigureResolver.resolve(card.id, bundle.figures, profile))
        } else {
            null
        }
    val stale = trust.ratesAsOf?.let { GuideStaleness.isStale(it, nowMillis) } == true
    return GuideCardContent(card.id, card.title, card.answer, bundle.facets[card.facet].orEmpty(), trust, stale, full)
}
