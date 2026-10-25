package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.DraftIssueReason
import com.payslipmax.pdfparser.part2.domain.LinkBasis
import com.payslipmax.pdfparser.part2.domain.Part2Span

/**
 * App chrome copy for Part II Orders (docs/Plan/12_Part2Orders_PhasePlan.md). Code names, groups, ribbon and case titles are content
 * from the Guide bundle, never copy here. Every later Part II string, including notification and print-job text, is added here.
 */
object Part2Strings {
    // Premium Features catalog row.
    const val catalogIcon = "📝"
    const val catalogTitle = "Part II Orders"
    const val catalogDescription = "Draft the order slip for your clerk, check it against the manual and keep published orders in an encrypted vault"

    // Guide Home entry and the Part II home.
    const val entryEmoji = catalogIcon
    const val entryTitle = "Part II Orders"
    const val entrySubtitle = "Look up any ODP-2026 code: its name, group and manual page"
    const val homeSubtitle = "Army officers (ODP-2026)"

    // Search.
    const val searchPlaceholder = "Code, name or serial no."
    const val searchClear = "Clear search"
    const val searchHint = "Search all 303 codes by HRMS code, by name as the manual words it, or by serial such as 5.1."
    const val searchTooShort = "Type at least two letters or digits."
    const val searchNone = "No code matches. Try fewer words, or the serial number."

    // Lists and code pages.
    const val lookupOnly = "Lookup only"
    const val alsoSeeHeading = "Also see"
    const val ribbonsHeading = "Choose a group of orders"
    const val serialLabel = "Serial no."
    const val codesLabel = "HRMS codes"
    const val manualLabel = "In the manual"

    // Pinned by Part2StringsTest.
    const val groupNotPrinted = "Group not printed in ODP-2026"

    fun printedAsNote(
        code: String,
        printed: String,
    ): String = "The slip prints $printed, as HRMS publishes it. ODP-2026 lists $code."

    /** "Ch 5, p 114", "Ch 5, pp 114-117" or "Ch 2, 3, 5, pp 39-131": the chapters and printed pages a case covers. */
    fun span(
        chapters: List<Int>,
        firstPage: Int,
        lastPage: Int,
    ): String = "Ch ${chapters.joinToString(", ")}, " + if (firstPage == lastPage) "p $firstPage" else "pp $firstPage-$lastPage"

    fun codeCount(count: Int): String = if (count == 1) "1 code" else "$count codes"

    fun chapterAndPage(
        chapter: Int,
        page: Int,
    ): String = "Chapter $chapter, page $page of ODP-2026"

    fun groupLabel(letter: String): String = "Group $letter"

    /** A case or ribbon's second line: the span, then how many codes it holds. */
    fun tileLine(
        span: Part2Span,
        count: Int,
    ): String = "${span(span.chapters, span.firstPage, span.lastPage)} · ${codeCount(count)}"

    /** A serial's HRMS codes on one line (a variant serial has several). */
    fun codesLine(codes: List<String>): String = codes.joinToString(", ")

    // ---- Drafting (Premium): the entry, the drafts list and the locked panel ----
    const val draftThisOrder = "Draft this order"
    const val draftPremium = "Premium"
    const val lockedTitle = "Drafting is a Premium feature"
    const val lockedBody = "Premium drafts the slip for your clerk, checks it against the manual and keeps your drafts safe in the app. Looking codes up stays free."
    const val draftsHeading = "Your drafts"
    const val draftNotFinished = "Not finished"
    const val draftsUnreadable = "Some drafts could not be read on this device."
    const val deleteDraft = "Delete draft"
    const val deleteTitle = "Delete this draft?"
    const val deleteBody = "The draft is removed from this device. Orders you have filed are not touched."
    const val deleteConfirm = "Delete"
    const val deleteCancel = "Cancel"
    const val viewOnlyNote = "Your Premium has ended. You can still read and delete this draft, but not change it."

    fun draftRowTitle(codes: List<String>): String = codes.joinToString(", ")

    fun draftRowLine(
        span: String?,
        edited: String,
    ): String = (span ?: draftNotFinished) + " · edited " + edited

    fun dateSpan(
        from: String,
        to: String,
    ): String = "$from to $to"

    // ---- The leave form ----
    const val formTitle = "Draft the order"
    const val formSubtitle = "Your answers are saved as you type"
    const val orderLabel = "Order"
    const val requiredMark = "required"
    const val optionalMark = "optional"
    const val orderNoLabel = "Order no."
    const val orderDateLabel = "Order date"
    const val clearAnswer = "Clear"
    const val addAnotherHeading = "Add another occurrence to this event"
    const val removeOccurrence = "Remove this occurrence"
    const val noOptionChoice = "None"
    const val seeSlip = "See the slip"
    const val saveFailed = "This draft could not be saved on this device."
    const val missingDraft = "This draft is no longer on this device."

    fun sectionTitle(
        number: Int,
        code: String,
    ): String = "Occurrence $number: $code"

    /** What the slip preview says of a draft that is not complete yet: that, and what is still needed when that is known. */
    fun unfinishedLine(labels: List<String>): String = if (labels.isEmpty()) draftNotFinished else "$draftNotFinished. " + stillNeeded(labels)

    fun stillNeeded(labels: List<String>): String = "Still needed: " + labels.joinToString(", ")

    /** The wording of a form question, by the id its rule family gave it. An id with no wording is a gap the contract test catches. */
    fun question(id: String): String =
        when (id) {
            "ration" -> "Are you on ration strength?"
            "rh_code" -> "Which Risk and Hardship rate code do you draw?"
            else -> id
        }

    fun hasQuestion(id: String): Boolean = question(id) != id

    /** Why an answer was refused, in words; the field's own label goes in front of it. */
    fun issue(reason: DraftIssueReason): String =
        when (reason) {
            DraftIssueReason.NOT_ANSWERED -> "needs an answer"
            DraftIssueReason.WRONG_KIND -> "is not the kind of answer expected"
            DraftIssueReason.TOO_LONG -> "is too long"
            DraftIssueReason.MARKUP -> "must be plain text, without < or >"
            DraftIssueReason.OUT_OF_RANGE -> "is out of range"
            DraftIssueReason.BAD_DATE -> "is not a date on the calendar"
            DraftIssueReason.DATES_REVERSED -> "is before the first day"
            DraftIssueReason.NOTHING_REQUESTED -> "has nothing to draft"
            DraftIssueReason.TOO_MANY -> "has too many occurrences"
            DraftIssueReason.UNKNOWN_CODE -> "is not a code this version can draft"
        }

    fun fieldIssue(
        label: String,
        reason: DraftIssueReason,
    ): String = "$label ${issue(reason)}"

    // ---- The slip preview ----
    const val slipTitle = "Slip for your clerk"
    const val slipSubtitle = "Check it against what HRMS shows. It carries no name or number of yours."
    const val editDraft = "Edit"
    const val lastOrderBlank = "Last Part II order: ____"
    const val fromLabel = "From"
    const val toLabel = "To"
    const val authorityLabel = "Authority"
    const val checksHeading = "Checks against the manual"
    const val ackHeading = "Tick to confirm you have read this"
    const val ackTick = "I have read this. The order may be rejected."
    const val undraftedHeading = "Orders that go with this and are not drafted yet"
    const val readyToPrint = "All checks are ticked. You can print the slip."
    const val seenInPractice = "Seen in practice"

    // ---- Printing and the HRMS check ----

    /** The name of the print job: it shows in the system print queue and is the saved PDF's name, so it names no code, date or number. */
    const val printJobName = "Part II slip"
    const val slipDocumentTitle = "Part II order slip"
    const val slipNotice = "Draft for your clerk to key into HRMS. It is not a Part II order and carries no name or number of yours."

    /** The label at the foot of each printed page; `{page}` and `{pages}` are filled in by the layout. */
    const val pageLabel = "Page {page} of {pages}"
    const val printButton = "Print the slip"
    const val printingNow = "Printing..."
    const val printBlocked = "Tick every check above to print."
    const val changedSincePrinted = "Changed since it was printed. Print it again before you hand it over."
    const val templateUpdatedSincePrinted = "The manual's template for this order has been updated since it was printed. Check the slip and print it again."
    const val hrmsCheckButton = "Check the HRMS draft"
    const val hrmsTitle = "Check the HRMS draft"
    const val hrmsSubtitle = "Compare each field with what HRMS shows. This step is optional."
    const val hrmsCodeLabel = "Code"
    const val hrmsNarrativeLabel = "Narrative"

    fun printedOnLine(date: String): String = "Printed on $date"

    fun hrmsRowHeading(
        number: Int,
        total: Int,
    ): String = "Row $number of $total"

    fun lastOrderLine(
        no: String,
        date: String,
    ): String = "Last Part II order: $no dated $date"

    fun orderTitle(
        number: Int,
        groups: List<String>,
    ): String = "Order $number" + if (groups.isEmpty()) "" else " · Group " + groups.joinToString(", ")

    fun dataLabel(number: Int): String = "Data $number"

    fun fieldHelp(
        help: String,
        required: Boolean,
    ): String = if (required) help else "$help ($optionalMark)"

    /** The From and To of a row on one line; a column the manual leaves empty is left out. */
    fun fromToLine(
        from: String,
        to: String,
    ): String = listOfNotNull(from.takeIf { it.isNotEmpty() }?.let { "$fromLabel $it" }, to.takeIf { it.isNotEmpty() }?.let { "$toLabel $it" }).joinToString(" · ")

    fun dataLine(
        number: Int,
        text: String,
    ): String = "${dataLabel(number)}: $text"

    fun authorityLine(text: String): String = "$authorityLabel: $text"

    /** A certificate prints exactly as the manual words it: its own text already starts with the word CERTIFICATE, so no label of ours goes in front. */
    fun certificateLine(text: String): String = text

    fun undraftedLine(
        code: String,
        source: String,
    ): String = "$code ($source)"

    /** Where a check or a linked order comes from: the manual's page and note, or practice, which is never passed off as the manual. */
    fun citation(
        basis: LinkBasis,
        page: Int?,
        note: String?,
    ): String =
        when {
            basis == LinkBasis.PRACTICE -> seenInPractice
            page == null -> "ODP-2026"
            note.isNullOrBlank() -> "ODP-2026, p $page"
            else -> "ODP-2026, p $page, note $note"
        }

    /** What a check says, by the rule's id; null for an id with no wording (the contract test fails on that). */
    fun finding(ruleId: String): String? =
        when (ruleId) {
            "al_year_cap" -> "Annual leave, extended annual leave and leave taken earlier this year come to more than 60 days. The manual says such an order will be rejected."
            "al_affix_limit" -> "Prefix and suffix together are more than 5 days. The unit must check this."
            "cl_combination" -> "Casual leave cannot be combined with annual leave. The manual says such an order will be rejected."
            "spcl_over_30" -> "Special casual leave is granted for up to 30 days."
            "grant_quoted_annual", "grant_quoted_rejoin", "grant_quoted_recall" -> "Quote the order that granted the leave once it is published. Until then it prints blank."
            else -> Part2CancelStrings.finding(ruleId)
        }

    /**
     * The iOS camera prompt (Info.plist `NSCameraUsageDescription`), the one copy Apple requires in the app's plist. `Part2CameraUsageTest`
     * keeps the two equal. The Android scanner needs no such text: it runs in Google Play services.
     */
    const val cameraUsageDescription =
        "PayslipMax uses the camera only when you scan a published Part II order into your vault. Pages are stored encrypted on this device and are never added to your Photos."

    const val findingUnknown = "This order needs a check the app cannot word yet. Compare it with the manual."
}
