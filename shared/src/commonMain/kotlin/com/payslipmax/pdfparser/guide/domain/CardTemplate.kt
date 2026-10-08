package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideCard

/**
 * A card's bullet sections as the card screen shows them. [figureTemplates] are the bullets that name profile
 * placeholders (for example "Level {level} = Rs {food_rate}/day"): they are never shown raw. The "your figure" line
 * (phase E6) is drawn from the bundle's figures by [PersonalFigureResolver], not from these bullets, so all four
 * personal cards read the same way. Without a profile the rest of the card is complete on its own.
 */
data class GuideCardBody(
    val key: List<String>,
    val attach: List<String>,
    val watch: List<String>,
    val figureTemplates: List<String>,
)

/** Splits a card's placeholder bullets from its visible ones. Plain string scans, so it stays linear on Kotlin/Native. */
object CardTemplate {
    private const val PLACEHOLDER_OPEN = '{'

    fun hasPlaceholder(text: String): Boolean = PLACEHOLDER_OPEN in text

    fun body(card: GuideCard): GuideCardBody =
        GuideCardBody(
            key = card.key.filterNot(::hasPlaceholder),
            attach = card.attach.filterNot(::hasPlaceholder),
            watch = card.watch.filterNot(::hasPlaceholder),
            figureTemplates = (card.key + card.attach + card.watch).filter(::hasPlaceholder),
        )
}
