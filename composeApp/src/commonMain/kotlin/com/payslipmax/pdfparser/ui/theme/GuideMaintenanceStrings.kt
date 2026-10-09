package com.payslipmax.pdfparser.ui.theme

/**
 * Copy for the Guide's "what changed" and rule-history views (maintenance phase M4). Kept apart from [GuideStrings], which
 * is near its size limit. The change-log text itself, and card titles, are bundle content and never appear here.
 */
object GuideMaintenanceStrings {
    const val chipUpdated = "Updated"
    const val whatsNewTitle = "What's new"
    const val whatsNewSubtitle = "Changes to the rule cards"
    const val seeCurrentRule = "See current rule"
    const val seeCurrentRuleDescription = "Open the rule that applies now"
    const val replacedNotice = "This rule no longer applies to new claims. It is kept for claims made under it."
    const val openCardHint = "Open card"

    // The marker line in a shared claim note of a replaced card: "<shareReplacedOn> 15 Nov 2026. <shareReplacedApplies>"
    const val shareReplacedOn = "Replaced on"
    const val shareReplacedApplies = "It applies only to claims for earlier periods."

    /** The Guide Home row, named for the month of the newest entry. */
    fun whatsNewRow(latestDate: String): String = "What's new in the Guide (${GuideStrings.yearMonth(latestDate.take(YEAR_MONTH_LENGTH))})"

    fun chipReplacedOn(date: String): String = "Replaced on ${day(date)}"

    /** The link on a card that replaced an older rule; [before] is the day the older rule stopped applying. */
    fun earlierRule(before: String?): String = if (before == null) "Earlier rule" else "Earlier rule (before ${day(before)})"

    /** "2026-11-15" as "15 Nov 2026"; a value that is not a full date is returned as it came. */
    fun day(value: String): String {
        val isDate = value.length == DATE_LENGTH && value[YEAR_MONTH_LENGTH - 3] == '-' && value[DATE_LENGTH - 3] == '-'
        val dayOfMonth = if (isDate) value.substring(DATE_LENGTH - 2).toIntOrNull()?.takeIf { it in 1..MAX_DAY } else null
        val monthYear = GuideStrings.yearMonth(value.take(YEAR_MONTH_LENGTH))
        return if (dayOfMonth == null || monthYear == value.take(YEAR_MONTH_LENGTH)) value else "$dayOfMonth $monthYear"
    }

    private const val YEAR_MONTH_LENGTH = 7
    private const val DATE_LENGTH = 10
    private const val MAX_DAY = 31
}
