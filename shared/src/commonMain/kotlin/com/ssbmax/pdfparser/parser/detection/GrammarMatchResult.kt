package com.ssbmax.pdfparser.parser.detection

/**
 * Result of executing a deterministic grammar matcher function against a token stream.
 * Pure and side-effect free.
 */
data class GrammarMatchResult(
    val isMatch: Boolean,
    val matchedFingerprints: List<String> = emptyList(),
    val rejectedReasons: List<String> = emptyList(),
)
