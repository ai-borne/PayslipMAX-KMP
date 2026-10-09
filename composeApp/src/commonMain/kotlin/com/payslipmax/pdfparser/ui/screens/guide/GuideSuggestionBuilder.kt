package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.GuideIndex
import com.payslipmax.pdfparser.guide.domain.GuideSuggestion
import com.payslipmax.pdfparser.guide.domain.GuideSuggestionLabels
import com.payslipmax.pdfparser.guide.domain.GuideSuggestionParts
import com.payslipmax.pdfparser.ui.theme.AppStringsSupport
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings

/** The subject and body of a suggestion email; the recipient is always [AppStringsSupport.supportEmail]. */
internal data class GuideSuggestionMail(
    val subject: String,
    val body: String,
)

private val SuggestionLabels =
    GuideSuggestionLabels(
        subjectTag = GuideMaintenanceStrings.suggestSubjectTag,
        header = GuideMaintenanceStrings.suggestMailHeader,
        card = GuideMaintenanceStrings.suggestMailCard,
        title = GuideMaintenanceStrings.suggestMailTitle,
        bundle = GuideMaintenanceStrings.suggestMailBundle,
        revision = GuideMaintenanceStrings.suggestMailRevision,
        app = GuideMaintenanceStrings.suggestMailApp,
        suggestion = GuideMaintenanceStrings.suggestMailSuggestion,
    )

/**
 * The email for a suggestion about [cardId], or null for a card the bundle does not hold. Only the card's id, title and
 * revision, the bundle's date, the app version and the user's [text] go in; see [GuideSuggestionParts].
 */
internal fun GuideIndex.suggestionMail(
    cardId: String,
    text: String,
    appVersion: String,
): GuideSuggestionMail? {
    val card = card(cardId) ?: return null
    val parts = GuideSuggestionParts(card.id, card.title, bundle.generated, card.rev, appVersion, text)
    return GuideSuggestionMail(GuideSuggestion.subject(parts, SuggestionLabels), GuideSuggestion.body(parts, SuggestionLabels))
}

/** Hands the suggestion to the user's mail app, once, from a button tap. The app sends nothing itself and reports nothing. */
internal fun GuideViewModel.suggest(
    cardId: String,
    text: String,
    platform: GuidePlatform,
) {
    val mail = suggestionMail(cardId, text) ?: return
    platform.email(AppStringsSupport.supportEmail, mail.subject, mail.body)
}
