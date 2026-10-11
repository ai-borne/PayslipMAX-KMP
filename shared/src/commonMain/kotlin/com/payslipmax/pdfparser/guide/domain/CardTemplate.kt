package com.payslipmax.pdfparser.guide.domain

/**
 * Cards carry no template placeholders (the validator rejects a braced name such as "Rs {food_rate}", which would be shown
 * raw); the "your figure" line is drawn from the bundle's figures by [PersonalFigureResolver]. Plain string scans, so it
 * stays linear on Kotlin/Native.
 */
object CardTemplate {
    private const val PLACEHOLDER_OPEN = '{'

    fun hasPlaceholder(text: String): Boolean = PLACEHOLDER_OPEN in text
}
