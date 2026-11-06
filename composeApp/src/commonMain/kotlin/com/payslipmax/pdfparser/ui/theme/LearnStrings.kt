package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.Part2ColumnPosition

/**
 * App chrome copy for Learn about Part II (docs/Plan/13_OrdersBank_PhasePlan.md, Phase 2). Casualty names, groups, ribbon and case
 * titles, column captions and the plain-English pages are content from the Guide bundle, never copy here. The Orders Bank's own
 * strings go in their own object when that screen exists.
 */
object LearnStrings {
    // Guide Home: the label of the user's own group, below the handbook areas (a different manual), and the note beside it that says it is private.
    const val sectionHeading = "My records"
    const val sectionSubtext = "Stored only on this phone"

    // Guide Home's one Part II tile, and the hub screen it opens (Learn and the Orders Bank).
    const val hubEmoji = "📜"
    const val hubTitle = "My Part II Orders"
    const val hubTileSubtitle = "Learn the process, keep each order"

    // Guide Home entry and the Learn home.
    const val entryEmoji = "📖"
    const val entryTitle = "Learn about Part II"
    const val entrySubtitle = "What the documentation requires for each order, in plain words"
    const val homeSubtitle = "Army officers, as per documentation requirements"

    /** Learn opened from "Add a casualty" on the attach form: a picker for that order, so it says what a tap does. */
    const val pickTitle = "Add a casualty"
    const val pickSubtitle = "Tap a casualty to add it to this order"

    // Search.
    const val searchPlaceholder = "Code, name or serial no."
    const val searchClear = "Clear search"
    const val searchHint = "Search all 303 casualties by HRMS code, by name as the manual words it, or by serial such as 5.1."
    const val searchTooShort = "Type at least two letters or digits."
    const val searchNone = "No casualty matches. Try fewer words, or the serial number."

    // Lists.
    const val groupNotPrinted = "Group not given in the documentation"

    fun groupLabel(letter: String): String = "Group $letter"

    fun casualtyCount(count: Int): String = if (count == 1) "1 casualty" else "$count casualties"

    /** A serial's HRMS codes on one line (a variant serial has several). */
    fun codesLine(codes: List<String>): String = codes.joinToString(", ")

    // A casualty page.
    const val manualLabel = "In the manual"
    const val serialLabel = "Serial no."
    const val columnsHeading = "What the order needs"
    const val columnsNone = "This order uses none of the From, To or Data columns."
    const val needRequired = "Required"
    const val needOptional = "Optional"
    const val aboutHeading = "In plain words"
    const val aboutWhat = "What it is"
    const val aboutWhen = "When you will see it"
    const val aboutReady = "What to keep ready"
    const val aboutWatch = "One thing to watch"
    const val relatedHeading = "Usually published with"
    const val relatedPractice = "In practice"
    const val addToBank = "Add to Orders Bank"

    fun chapterAndPage(
        chapter: Int,
        page: Int,
    ): String = "Chapter $chapter, page $page"

    /** The word for one of an order's six columns: From, To, then Data 1 to 4. */
    fun columnLabel(position: Int): String =
        when (position) {
            Part2ColumnPosition.FROM -> "From"
            Part2ColumnPosition.TO -> "To"
            else -> "Data ${position - Part2ColumnPosition.DATA_FIRST + 1}"
        }

    /** What a required column says when the manual prints no caption for it. */
    fun requiredNoCaption(page: Int): String = "required (see manual, page $page)"

    fun relatedBasis(page: Int?): String = if (page == null) relatedPractice else "In the manual, page $page"
}
