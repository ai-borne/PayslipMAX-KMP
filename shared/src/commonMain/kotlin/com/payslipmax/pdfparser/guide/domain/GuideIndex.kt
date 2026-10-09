package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideArea
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.guide.model.GuideCase

/**
 * Id lookups over a loaded bundle, built once per load (the bundle never changes while the app runs), so a
 * screen never scans all 402 cards to draw one feed.
 */
class GuideIndex(
    val bundle: GuideBundle,
) {
    private val areasById = bundle.nav.associateBy { it.id }
    private val casesById = bundle.nav.flatMap { it.cases }.associateBy { it.id }
    private val areaByCaseId = bundle.nav.flatMap { area -> area.cases.map { it.id to area } }.toMap()
    private val cardsById = bundle.cards.associateBy { it.id }

    /** The hand-written change log; built on first use, so a Guide with no change entry pays nothing. */
    val changeLog: GuideChangeLog by lazy { GuideChangeLog(bundle.changes) }

    /** The chain of dated rules behind each card; built on first use. */
    val history: GuideRuleHistory by lazy { GuideRuleHistory(bundle.cards) }

    fun area(id: String): GuideArea? = areasById[id]

    fun case(id: String): GuideCase? = casesById[id]

    /** The area whose tile leads to [caseId]. */
    fun areaOfCase(caseId: String): GuideArea? = areaByCaseId[caseId]

    fun card(id: String): GuideCard? = cardsById[id]

    /**
     * The one place a card's chips are decided, for a feed row, a search result, a pinned row and the card screen alike.
     * A replaced card never also reads "Updated": the replacement is the stronger message.
     */
    fun trust(card: GuideCard): GuideTrust =
        GuideTrust.of(card, bundle.ratesAsOf).copy(
            updated = !card.isReplaced && changeLog.changedInLatest(card.id),
            replacedUntil = history.replacedOn(card.id),
        )
}
