package com.payslipmax.pdfparser.part2.domain

/** What a group letter means, from para 3 of the manual (carried in the bundle). */
data class Part2GroupInfo(val letter: String, val meaning: String)

/** The chapters and printed pages a case covers, for its "Ch 5, pp 114-117" line. Derived from its code pages, never authored. */
data class Part2Span(val chapters: List<Int>, val firstPage: Int, val lastPage: Int)

/**
 * One serial in a list. [openCode] is the code its page opens on: the one a search matched, else the first. [group] is the letter, or null
 * where the manual prints none (3.28 and 11.8); none is ever inferred.
 */
data class Part2CodeRow(
    val serial: String,
    val codes: List<String>,
    val openCode: String,
    val name: String,
    val group: String?,
    val lookupOnly: Boolean,
    /** The code to draft an order for when this row is one of a built case's own serials that has a template; null on every other row. */
    val draftCode: String? = null,
)

/** A code the officer can start a draft with, and the manual's name for it. */
data class Part2DraftOption(val code: String, val name: String)

/** A code the slip prints differently from the manual's Annexure II (REJOINAL is printed REJOIN, as HRMS publishes it). */
data class Part2PrintedAs(val code: String, val printed: String)

/** A code page: the minimal lookup fields (code, manual name, group with its meaning, chapter, manual page). */
data class Part2CodeContent(
    val serial: String,
    val codes: List<String>,
    val name: String,
    val group: Part2GroupInfo?,
    val chapter: Int,
    val page: Int,
    val printedAs: List<Part2PrintedAs>,
)

data class Part2CaseTile(val id: String, val title: String, val span: Part2Span, val codeCount: Int)

data class Part2CaseContent(
    val id: String,
    val title: String,
    val span: Part2Span,
    val rows: List<Part2CodeRow>,
    /** Codes homed in another case that the layout lists here too. */
    val also: List<Part2CodeRow>,
)

data class Part2RibbonTile(val id: String, val title: String, val caseCount: Int)

data class Part2RibbonContent(val id: String, val title: String, val cases: List<Part2CaseTile>)

/** What the search shows below its field. */
sealed interface Part2SearchResult {
    data object Idle : Part2SearchResult

    /** Typed, but too short to search. */
    data object TooShort : Part2SearchResult

    /** Matches, best first; an empty list is "no results". */
    data class Results(val rows: List<Part2CodeRow>) : Part2SearchResult
}
