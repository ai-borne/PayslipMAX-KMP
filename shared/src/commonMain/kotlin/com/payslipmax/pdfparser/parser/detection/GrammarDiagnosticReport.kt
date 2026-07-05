package com.payslipmax.pdfparser.parser.detection

/**
 * Structured diagnostic telemetry output for every document parsing operation.
 * Provides complete explainability for matched strategies and rejected candidates.
 */
data class GrammarDiagnosticReport(
    val selectedFamily: GrammarFamily,
    val selectedPriority: Int,
    val isKnownGrammar: Boolean,
    val matchedFingerprints: List<String> = emptyList(),
    val rejectedCandidates: Map<String, List<String>> = emptyMap(),
    val selectedStrategies: Map<String, String> = emptyMap(),
    val validationStatus: String = "PENDING",
    val warnings: List<String> = emptyList(),
    /** Why this family was selected, e.g. "Date mapping (Mar 2025+)" or "Statement period unavailable; fallback detector used". */
    val selectionReason: String = "",
)
