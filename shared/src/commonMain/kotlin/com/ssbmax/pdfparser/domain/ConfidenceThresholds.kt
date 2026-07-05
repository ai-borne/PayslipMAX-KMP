package com.ssbmax.pdfparser.domain

/**
 * SSOT for the confidence cut-off shared by the arithmetic solver (which sets [ParsedPayslip.needsReview])
 * and the Phase 5 correction UI (which decides which fields to surface for review). One number, one place.
 */
object ConfidenceThresholds {
    /** A field whose confidence is strictly below this is considered uncertain and surfaced for correction. */
    const val REVIEW_THRESHOLD = 0.7f

    /**
     * Fixed confidence assigned to a field recovered by the Tier 6 Gemma fallback. It is a floor, not a
     * computed score — an LLM-guessed field is always uncertain regardless of how confident the geometry
     * solver was before Gemma ran, so it must sit below [REVIEW_THRESHOLD] to surface for review.
     */
    const val GEMMA_FALLBACK_CONFIDENCE = 0.5f

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

/**
 * True when [fieldKey] was recovered by the Tier 6 Gemma fallback rather than the deterministic
 * geometry solver. Distinct from [isFieldLowConfidence] — every Gemma-sourced field is already
 * low-confidence by construction ([ConfidenceThresholds.GEMMA_FALLBACK_CONFIDENCE] sits below
 * [ConfidenceThresholds.REVIEW_THRESHOLD]), but the UI needs to tell "AI inferred this" apart from
 * "the deterministic solver was merely unsure" instead of both collapsing into the same warning icon.
 */
fun ParsedPayslip.isFieldGemmaSourced(fieldKey: String): Boolean = fieldSource[fieldKey] == FieldSource.GEMMA_FALLBACK
