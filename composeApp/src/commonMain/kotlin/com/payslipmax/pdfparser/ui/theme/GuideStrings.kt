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

    // Search.
    const val searchTitle = "Search"
    const val searchOpen = "Search the Claim Guide"
    const val searchPlaceholder = "Topic or rule number"
    const val searchClear = "Clear search"
    const val searchHint = "Search by a word, or a rule number such as 177B or Rule 114."
    const val searchTooShort = "Type at least two letters or digits."
    const val searchNone = "No cards match. Try fewer words, or a rule number."

    // Feed and breadcrumb.
    const val breadcrumbHome = "Guide"
    const val breadcrumbSeparator = "›"
    const val facetAllLabel = "All"

    // Card screen.
    const val sectionKeyPoints = "Key points"
    const val sectionAttach = "Attach"
    const val sectionWatchOut = "Watch out"
    const val sectionAuthority = "Authority"
    const val details = "Details"
    const val detailsShown = "Shown"
    const val detailsHidden = "Hidden"
    const val bullet = "•"
    const val cardDisclaimer = "Guidance from published rules, not a sanction. Your controlling officer and PCDA(O) decide the claim."

    fun topicCount(count: Int): String = if (count == 1) "1 topic" else "$count topics"

    fun cardCount(count: Int): String = if (count == 1) "1 card" else "$count cards"

    fun resultCount(count: Int): String = if (count == 1) "1 result" else "$count results"

    fun facetAll(count: Int): String = "$facetAllLabel $count"

    /** A facet chip; [label] is bundle content (for example "How much"). */
    fun facetChip(
        label: String,
        count: Int,
    ): String = "$label $count"

    /** Read aloud for a facet chip, so the bare count is never read on its own. */
    fun facetChipDescription(
        label: String,
        count: Int,
    ): String = "$label, ${cardCount(count)}"

    fun alsoRelevant(homeCaseTitle: String): String = "Also relevant here. Main home: $homeCaseTitle"

    fun breadcrumbDescription(label: String): String = "Up to $label"
}
