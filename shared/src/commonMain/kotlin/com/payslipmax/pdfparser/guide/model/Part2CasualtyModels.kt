package com.payslipmax.pdfparser.guide.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The words the bundle uses for a column's need and a link's basis: their one home, so the validator and the Learn index cannot drift apart. */
object Part2Words {
    const val REQUIRED = "required"
    const val OPTIONAL = "optional"
    const val NONE = "none"
    const val MANUAL = "manual"
    const val PRACTICE = "practice"
}

/**
 * What one Annexure III column of an order needs, in the manual's own words (`part2/columns.json`, built by `part2_columns*.py`).
 * [need] is `required`, `optional` or `none`; a `none` column has an empty [caption]. Content, not UI copy. Plain strings only, so the
 * release build needs no keep rule (`scripts/check_r8_guide.py` proves it).
 */
@Serializable
data class Part2Column(
    val need: String,
    val caption: String,
)

/** The six columns of one serial: From, To and Data 1 to 4. The serial's codes and page live in [Part2CodePage], never repeated here. */
@Serializable
data class Part2ColumnRow(
    val serial: String,
    val from: Part2Column,
    val to: Part2Column,
    val data: List<Part2Column>,
)

/** A plain-English page (`part2/about.json`) that covers [serials], the first of which is the [id]: four short parts. */
@Serializable
data class Part2AboutPage(
    val id: String,
    val serials: List<String>,
    val what: String,
    @SerialName("when") val whenSeen: String,
    val ready: String,
    val watch: String,
)

/** The entries usually published with [code] (`part2/related.json`). */
@Serializable
data class Part2RelatedRow(
    val code: String,
    @SerialName("with") val withCodes: List<Part2RelatedLink>,
)

/** One link: [basis] is `manual` (then [page] is the printed page that says so) or `practice` (no page). */
@Serializable
data class Part2RelatedLink(
    val code: String,
    val basis: String,
    val page: Int? = null,
)
