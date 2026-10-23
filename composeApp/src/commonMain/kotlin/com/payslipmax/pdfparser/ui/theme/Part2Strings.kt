package com.payslipmax.pdfparser.ui.theme

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
}
