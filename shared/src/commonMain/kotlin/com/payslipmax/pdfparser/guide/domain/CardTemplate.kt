package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideCard

/**
 * A card's bullet sections as the card screen shows them. Cards carry no template placeholders (the validator rejects a
 * braced name such as "Rs {food_rate}", which would be shown raw); the "your figure" line is drawn from the bundle's
 * figures by [PersonalFigureResolver].
 */
data class GuideCardBody(
    val key: List<String>,
    val attach: List<String>,
    val watch: List<String>,
)

/** Plain string scans, so it stays linear on Kotlin/Native. */
object CardTemplate {
    private const val PLACEHOLDER_OPEN = '{'

    fun hasPlaceholder(text: String): Boolean = PLACEHOLDER_OPEN in text

    fun body(card: GuideCard): GuideCardBody = GuideCardBody(key = card.key, attach = card.attach, watch = card.watch)
}
