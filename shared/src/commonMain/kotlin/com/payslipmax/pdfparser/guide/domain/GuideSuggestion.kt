package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.domain.SUPPORT_MESSAGE_MAX_CHARS

/** The words around a suggestion email. They are passed in, so the copy lives only in the app's `GuideMaintenanceStrings`. */
data class GuideSuggestionLabels(
    val subjectTag: String,
    val header: String,
    val card: String,
    val title: String,
    val bundle: String,
    val revision: String,
    val app: String,
    val suggestion: String,
)

/**
 * Everything a suggestion email may contain. There is deliberately no field for a figure, a profile value, a pin, a change-log
 * line, a payslip or any other part of a card's text, so none of them can reach the email by type. [text] is the user's own
 * words; the rest is public: the card, the date of the Guide data it came from, its revision and the app version.
 */
data class GuideSuggestionParts(
    val cardId: String,
    val cardTitle: String,
    val bundleGenerated: String,
    val cardRev: String,
    val appVersion: String,
    val text: String,
)

/**
 * Builds the email a user can send to suggest a correction to a Guide card. Pure: the app sends nothing itself, it only hands
 * the subject and body to the user's mail app. The user's text is trimmed and capped, never rewritten, because a correction
 * is often a rupee figure or an order number.
 */
object GuideSuggestion {
    fun subject(
        parts: GuideSuggestionParts,
        labels: GuideSuggestionLabels,
    ): String = "${labels.subjectTag} ${parts.cardId}"

    fun body(
        parts: GuideSuggestionParts,
        labels: GuideSuggestionLabels,
    ): String {
        val facts =
            listOf(
                labels.card to parts.cardId,
                labels.title to parts.cardTitle,
                labels.bundle to parts.bundleGenerated,
                labels.revision to parts.cardRev,
                labels.app to parts.appVersion,
            ).filter { (_, value) -> value.isNotBlank() }
        return listOf(labels.header, facts.joinToString("\n") { (label, value) -> "$label ${value.trim()}" }, "${labels.suggestion}\n${clean(parts.text)}")
            .joinToString("\n\n")
    }

    /** The user's text, trimmed and capped. A cut that would split a surrogate pair drops the half pair. */
    fun clean(text: String): String {
        val capped = text.trim().take(SUPPORT_MESSAGE_MAX_CHARS)
        return (if (capped.lastOrNull()?.isHighSurrogate() == true) capped.dropLast(1) else capped).trim()
    }
}
