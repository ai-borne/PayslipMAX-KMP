package com.ssbmax.pdfparser.domain

/**
 * SSOT for the confidence cut-off shared by the arithmetic solver (which sets [ParsedPayslip.needsReview])
 * and the Phase 5 correction UI (which decides which fields to surface for review). One number, one place.
 */
object ConfidenceThresholds {
    /** A field whose confidence is strictly below this is considered uncertain and surfaced for correction. */
    const val REVIEW_THRESHOLD = 0.7f

    /** Maximum ₹ gap between displayed item sum and printed footer total before a mismatch banner fires. */
    const val ITEM_SUM_TOLERANCE = 2.0
}

/**
 * True when the parser recorded a confidence for [fieldKey] that is below [ConfidenceThresholds.REVIEW_THRESHOLD].
 * Fields with no recorded confidence (e.g. trivially-zero line items) are treated as certain.
 */
fun ParsedPayslip.isFieldLowConfidence(fieldKey: String): Boolean {
    val confidence = fieldConfidence[fieldKey] ?: return false
    return confidence < ConfidenceThresholds.REVIEW_THRESHOLD
}
