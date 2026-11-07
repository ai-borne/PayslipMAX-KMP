package com.payslipmax.pdfparser.part2.domain

/** What a group letter means, from para 3 of the manual (carried in the bundle). */
data class Part2GroupInfo(val letter: String, val meaning: String)

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
)

/** Which of an order's six columns a line is about: [FROM], [TO], then [DATA_FIRST] up to [DATA_FIRST] + 3. The words for them are UI copy. */
object Part2ColumnPosition {
    const val FROM = 0
    const val TO = 1
    const val DATA_FIRST = 2
}

/** `required` or `optional` in the bundle; a `none` column has no line at all. */
enum class Part2Need { REQUIRED, OPTIONAL }

/** One column an order uses for a serial. [caption] is the manual's own words, or empty where the manual prints none for a required column. */
data class Part2ColumnLine(val position: Int, val need: Part2Need, val caption: String)

/** The plain-English page of a serial, four short parts (our own wording, not the manual's). */
data class Part2AboutContent(val what: String, val whenSeen: String, val ready: String, val watch: String)

/** Why two casualties are published together: the manual says so (with its printed page), or it is how orders usually come. */
enum class Part2LinkBasis { MANUAL, PRACTICE }

data class Part2RelatedLine(val code: String, val name: String, val basis: Part2LinkBasis, val page: Int?)

/** A casualty page: the manual's data, its columns, and (only where the bundle has them) the plain-English page and the usual companions. */
data class Part2CodeContent(
    val serial: String,
    val codes: List<String>,
    val name: String,
    val group: Part2GroupInfo?,
    val chapter: Int,
    val page: Int,
    val columns: List<Part2ColumnLine>,
    val about: Part2AboutContent?,
    val related: List<Part2RelatedLine>,
)

data class Part2RibbonTile(val id: String, val title: String, val serialCount: Int)

/** The casualties one case homes, under the case's name as a heading. */
data class Part2RibbonSection(val title: String, val rows: List<Part2CodeRow>)

/** A ribbon as one flat list: its cases are headings, never a level of their own. */
data class Part2RibbonContent(val id: String, val title: String, val sections: List<Part2RibbonSection>)

/** What the search shows below its field. */
sealed interface Part2SearchResult {
    data object Idle : Part2SearchResult

    /** Typed, but too short to search. */
    data object TooShort : Part2SearchResult

    /** Matches, best first; an empty list is "no results". */
    data class Results(val rows: List<Part2CodeRow>) : Part2SearchResult
}
