package com.payslipmax.pdfparser.guide.domain

/** The words around a share note. They are passed in, so the copy lives only in the app's `GuideStrings`. */
data class GuideShareLabels(
    val header: String,
    val answer: String,
    val keyPoints: String,
    val attach: String,
    val watchOut: String,
    val authority: String,
    val unverified: String,
)

/**
 * Everything a share note may contain: the card's own public text and its cite. There is deliberately no field for
 * details, a personal figure, a name or a service number, so none of them can reach the note by type.
 */
data class GuideShareParts(
    val title: String,
    val answer: String,
    val key: List<String>,
    val attach: List<String>,
    val watch: List<String>,
    val cite: String,
    val unverified: Boolean,
)

/** Builds the plain-text "claim note" the user may share. Pure and fixed in layout; an empty section is left out. */
object GuideShareText {
    fun build(
        parts: GuideShareParts,
        labels: GuideShareLabels,
    ): String {
        val answer = "${labels.answer} ${parts.answer}" + if (parts.unverified) "\n${labels.unverified}" else ""
        val blocks =
            listOf(labels.header, parts.title, answer) +
                listOfNotNull(
                    bullets(labels.keyPoints, parts.key),
                    bullets(labels.attach, parts.attach),
                    bullets(labels.watchOut, parts.watch),
                    parts.cite.takeIf { it.isNotBlank() }?.let { "${labels.authority} $it" },
                )
        return blocks.joinToString("\n\n")
    }

    private fun bullets(
        heading: String,
        items: List<String>,
    ): String? = items.takeIf { it.isNotEmpty() }?.joinToString("\n", prefix = "$heading\n") { "- $it" }
}
