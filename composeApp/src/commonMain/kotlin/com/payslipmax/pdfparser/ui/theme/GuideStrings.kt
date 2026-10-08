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
    const val searchHint = "Search by topic, or by a rule number such as 177B or Rule 114."
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

    // Trust chips (E5). Each is backed by one bundle field; see GuideTrust.
    const val chipAmended = "Amended"
    const val chipUnverified = "Unverified point"
    const val chipNoOfficialSource = "No official source"
    const val unverifiedWarning = "This point is still being checked. Confirm it against the current order before you rely on it."

    // Premium preview: the locked half of a card.
    const val lockedTitle = "Key points, authority and details are in Premium"
    const val lockedBody = "The title and the one-line answer stay free."
    const val unlock = "Unlock with Premium"

    // Premium Features catalog row.
    const val catalogIcon = "📖"
    const val catalogTitle = "Claim Guide"
    const val catalogDescription = "Key points, the authority to cite and details for every pay and travel rule card"

    private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** "2026-01" as "Jan 2026"; a value that is not a year and month is returned as it came. */
    fun yearMonth(value: String): String {
        val month = value.substringAfter('-', "").toIntOrNull()
        val year = value.substringBefore('-')
        return if (month in 1..12 && year.length == 4) "${monthNames[month!! - 1]} $year" else value
    }

    fun chipRatesAsOf(value: String): String = "Rates as of ${yearMonth(value)}"

    /** The nudge on a rate card whose rates are old; asks the user to check the current order, never states a new rate. */
    fun staleRatesNudge(value: String): String =
        "These rates are from ${yearMonth(value)} and may have changed since. Check the latest order before you claim."

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

    // Your figure (phase E6). The amounts come from the bundle's figures and the payslip, never from this file.
    const val sectionYourFigure = "Your figure"
    const val cityHigherRate = "higher-rate city"
    const val cityOtherPlaces = "other places"

    fun figureADay(amount: String): String = "$amount a day"

    fun figureAMonth(amount: String): String = "$amount a month"

    fun figureAbout(amount: String): String = "About $amount"

    fun figurePercentOfBasic(percent: Int): String = "$percent% of basic pay"

    fun figureFoodDetail(
        level: String,
        base: String,
        stepPercent: Int,
    ): String = "Level $level. Base $base" + (if (stepPercent > 0) " plus $stepPercent% for DA" else "") + "."

    fun figureCtgDetail(percent: Int): String = "$percent% of your latest basic pay."

    fun figureTransportDetail(
        level: String,
        base: String,
        daPercent: Int,
        city: String?,
    ): String = "Level $level. Base $base plus $daPercent% DA on it" + (city?.let { ", $it" } ?: "") + "."

    /** The link on a Pay Audit finding that opens the Guide card for its rule (E7). */
    const val payAuditSeeRule = "Read the rule"

    fun figureHraDetail(
        hraClass: String,
        daPercent: Int,
    ): String = "Your payslip HRA fits a class $hraClass city at DA $daPercent%."

    /** The assumption the card could not know, in the bundle's words; empty when the figure needs none. */
    fun figureAssumes(assumption: String): String = if (assumption.isBlank()) "" else " Assumes a $assumption."

    /** Where the number comes from: the payslip month and DA it was read from, and when the rate started. */
    fun figureFootnote(
        payslipMonth: String,
        daPercent: Int?,
        since: String,
    ): String = "From your $payslipMonth payslip" + (daPercent?.let { ", DA $it%" } ?: "") + ". Rate in force from $since."
}
