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
    /** "Replaced on": followed by the date, then [replacedApplies]. */
    val replacedOn: String,
    val replacedApplies: String,
)

/**
 * Everything a share note may contain: the card's own public text and its cite. There is deliberately no field for
 * details, a personal figure, a name or a service number, so none of them can reach the note by type. [replacedOn] is the day
 * a newer rule took over (already written as a date), set only on a replaced card: a note is plain text with no chips, so
 * without it a forwarded old rule could pass as current. It is a date, never change-log text.
 */
data class GuideShareParts(
    val title: String,
    val answer: String,
    val key: List<String>,
    val attach: List<String>,
    val watch: List<String>,
    val cite: String,
    val unverified: Boolean,
    val replacedOn: String? = null,
)

/** Builds the plain-text "claim note" the user may share. Pure and fixed in layout; an empty section is left out. */
object GuideShareText {
    fun build(
        parts: GuideShareParts,
        labels: GuideShareLabels,
    ): String {
        val replaced = parts.replacedOn?.let { "\n${labels.replacedOn} $it. ${labels.replacedApplies}" }.orEmpty()
        val answer = "${labels.answer} ${parts.answer}" + replaced + if (parts.unverified) "\n${labels.unverified}" else ""
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
