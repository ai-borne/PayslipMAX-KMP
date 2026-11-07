package com.payslipmax.pdfparser.utils

/**
 * How the printed Part II orders index looks, beside [PdfLayoutSpec] (the A4 page, margins and body type): the type styles, the ink and
 * rule colours (ARGB) and the gaps. The one home for them, so Android's `PdfDocument` and iOS's `UIGraphicsPDFRenderer` draw the same
 * page and no colour or size is written anywhere else.
 */
object IndexLayoutSpec {
    const val INK = 0xFF1A1A1AL
    const val QUIET_INK = 0xFF4D4D4DL
    const val RULE = 0xFF9E9E9EL

    /** Thickness of the hairline under a year heading and between two rows, in points. */
    const val RULE_WIDTH = 0.5

    /** Space between two rows, and before a year heading, in points. */
    const val ROW_GAP = 6
    const val YEAR_GAP = 12

    /** Space kept free at the bottom of each page for its page label. */
    const val FOOTER_RESERVE = 22

    /** One type style of the index: its point size, whether it is bold, the height of a line of it and its ink. */
    enum class Font(val size: Int, val bold: Boolean, val lineHeight: Int, val ink: Long) {
        TITLE(PdfLayoutSpec.TITLE_FONT_SIZE, true, 20, INK),
        HEADING(12, true, 18, INK),
        CODE(PdfLayoutSpec.BODY_FONT_SIZE, true, 15, INK),
        DETAIL(10, false, 13, QUIET_INK),
        FOOTER(9, false, 12, QUIET_INK),
    }

    /** Where body text must stop: above the page label. */
    val bodyBottom: Int get() = PdfLayoutSpec.contentBottom - FOOTER_RESERVE
}
