package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideChange

/**
 * The hand-written change log (`authoring/changes.txt`) as the app reads it. [entries] are newest first by date
 * (YYYY-MM-DD sorts as text), whatever order the bundle lists them in. Only the newest entry marks a card as updated, so an
 * "Updated" chip never lingers across releases. Plain list and set work only, no regex.
 */
class GuideChangeLog(changes: List<GuideChange>) {
    val entries: List<GuideChange> = changes.sortedByDescending { it.date }

    val latest: GuideChange? = entries.firstOrNull()

    private val latestCards: Set<String> = latest?.items?.flatMap { it.cards }?.toSet().orEmpty()

    /** The newest [count] entries, newest first. */
    fun latest(count: Int): List<GuideChange> = entries.take(count)

    /** True when the newest entry names [cardId]. */
    fun changedInLatest(cardId: String): Boolean = cardId in latestCards
}
