package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.guide.domain.GUIDE_NOTE_MAX_CHARS

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

    // Suggest a correction (M5): the card action, its dialog, and the words around the fields of the email it opens.
    const val suggest = "Suggest a correction"
    const val suggestDialogTitle = "Suggest a correction"
    const val suggestFieldLabel = "What should change?"
    const val suggestOpenEmail = "Open email"
    const val suggestNotice =
        "Your email app opens with this message ready, and nothing is sent until you press Send there. " +
            "It names this card and the Guide and app versions. " +
            "Please don't include your name, PAN, service number, or bank or account numbers."
    const val suggestSubjectTag = "[Guide]"
    const val suggestMailHeader = "Guide correction suggestion"
    const val suggestMailCard = "Card:"
    const val suggestMailTitle = "Title:"
    const val suggestMailBundle = "Guide data:"
    const val suggestMailRevision = "Card revision:"
    const val suggestMailApp = "App version:"
    const val suggestMailSuggestion = "Suggestion:"

    // Personal notes (M7): the card section, its editor, and the markers and lists around it.
    const val noteSectionTitle = "Your note"
    const val noteNotOfficial = "Private to you. Not official guidance."
    const val noteAdd = "Add a note"
    const val noteEdit = "Edit"
    const val noteDelete = "Delete"
    const val noteDeleteConfirm = "Delete note"
    const val noteDeleteTitle = "Delete this note?"
    const val noteDeleteBody = "It is removed from this device. It can only come back from a backup you made earlier."
    const val noteStale = "This card was updated since your note."
    const val noteCarriedHeading = "From an earlier version of this rule"
    const val noteCarriedStale = "Written for an earlier rule"
    const val noteEditorTitle = "Your private note"
    const val noteEditorLabel = "Write your note"
    const val noteEditorNotice =
        "Only you can see this note. It stays on this device, encrypted, and leaves it only inside your own backup. " +
            "It is not part of the official text."
    const val noteSave = "Save"
    const val noteFailed = "Your note could not be changed. Try again."
    const val noteMoveIncomplete = "Your note is saved here. The copy under the earlier rule could not be removed; delete it below."
    const val noteMarker = "Note"
    const val noteMarkerInNote = "In your note"
    const val notesRemovedTitle = "Notes on removed cards"
    const val notesRemovedSubtitle = "Your notes on cards no longer in the Guide"
    const val notesRemovedNone = "You have no notes on removed cards."

    fun noteCounter(length: Int): String = "$length / $GUIDE_NOTE_MAX_CHARS"

    fun notesRemovedRow(count: Int): String = "Notes on removed cards ($count)"

    fun notesUnreadable(count: Int): String = if (count == 1) "1 note could not be read" else "$count notes could not be read"

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
