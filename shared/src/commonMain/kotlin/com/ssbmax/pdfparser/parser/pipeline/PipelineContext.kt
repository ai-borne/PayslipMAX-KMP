package com.ssbmax.pdfparser.parser.pipeline

import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.TaxAndSavings
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.detection.GrammarDiagnosticReport

/**
 * Context state container carrying immutable inputs, intermediate extractions, and diagnostic telemetry
 * through the 7-layer parsing pipeline.
 */
data class PipelineContext(
    val tokenized: TokenizedPayslip,
    val filename: String,
    val cleanedFullTextRaw: String,
    val cleanedFullText: String,
    val monthNum: Int,
    val year: Int,
    val monthName: String,
    val dateStr: String,
    val grossPay: Double,
    val totalDeductions: Double,
    val netRemittance: Double,
    val officer: Officer? = null,
    val rawEarnings: Map<String, Double> = emptyMap(),
    val rawDeductions: Map<String, Double> = emptyMap(),
    val taxAndSavings: TaxAndSavings? = null,
    val report: GrammarDiagnosticReport? = null,
)
