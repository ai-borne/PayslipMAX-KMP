package com.ssbmax.pdfparser.domain

import kotlinx.serialization.Serializable

/**
 * Provenance sidecar for a parsed field: which tier actually produced the value carried in
 * [ParsedPayslip.fieldConfidence]. Distinguishes a value the deterministic geometry solver was
 * certain of from one Tier 6 Gemma inferred for an ambiguous/unmatched raw entry (SWOT weakness
 * in `docs/AI_INSIGHTS_PIPELINE.md` — "Gemma fallback is a black box relative to the deterministic
 * solver").
 */
@Serializable
enum class FieldSource {
    GEOMETRY,
    GEMMA_FALLBACK,
}
