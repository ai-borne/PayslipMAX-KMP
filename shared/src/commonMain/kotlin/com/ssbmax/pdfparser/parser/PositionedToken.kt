package com.ssbmax.pdfparser.parser

/**
 * Platform-independent intermediate representation (IR) of a single piece of text positioned on a
 * PDF page. Both Android (PDFBox `TextPosition`) and iOS (PDFKit bounds) emit `List<PositionedToken>`
 * so that all grid detection, row pairing, classification, and reconciliation can live in common
 * code and be unit-tested without a device.
 *
 * Coordinates are normalized to a single, top-down convention by the platform adapters before they
 * reach common code: [x]/[y] are the top-left corner of the token's bounding box, with [y] growing
 * downward. This removes the historical iOS (bottom-up) vs Android (top-down) divergence.
 *
 * Introduced in Phase 1 as the SSOT contract; populated by the platform extractors in Phase 2.
 */
data class PositionedToken(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    /** Right edge of the bounding box. */
    val right: Float get() = x + width

    /** Bottom edge of the bounding box. */
    val bottom: Float get() = y + height

    /** Horizontal center, used for column clustering. */
    val centerX: Float get() = x + width / 2f

    /** Vertical center, used for row clustering. */
    val centerY: Float get() = y + height / 2f
}
