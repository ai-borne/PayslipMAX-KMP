package com.payslipmax.pdfparser.ui.theme

/**
 * App chrome copy for the Claim Guide (docs/Plan/rule_cards/16_guide_phase_plan.md). Area, case, facet and card
 * text is content from the bundle, never copy here (owner decision 2026-10-07).
 */
object GuideStrings {
    const val tabLabel = "Guide"
    const val homeTitle = "Claim Guide"
    const val homeSection = "What do you need help with?"
    const val loading = "Opening the Claim Guide…"
    const val loadFailedTitle = "The Claim Guide could not open"
    const val loadFailedBody = "Your payslips are not affected. Try again. If it keeps failing, update the app."
    const val retry = "Try again"

    // Placeholder routes until phase E3 builds the feed and card screens; removed with them.
    const val comingNext = "The cards for this topic arrive in the next update."
    const val searchTitle = "Search"

    fun topicCount(count: Int): String = if (count == 1) "1 topic" else "$count topics"

    fun cardCount(count: Int): String = if (count == 1) "1 card" else "$count cards"
}
