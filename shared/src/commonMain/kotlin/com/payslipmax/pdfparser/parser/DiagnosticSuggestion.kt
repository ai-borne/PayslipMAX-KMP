package com.payslipmax.pdfparser.parser

import kotlinx.serialization.Serializable

/**
 * Tier 6 diagnostic hint identifying the single field most likely mis-extracted when
 * Tier 7 [SchemaValidator] fails arithmetic reconciliation. Read-only — never merged
 * into `earnings`/`deductions`, never auto-applied.
 */
@Serializable
data class DiagnosticSuggestion(
    val fieldKey: String,
    val reason: String,
)
