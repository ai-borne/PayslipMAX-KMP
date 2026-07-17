package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.parser.debug.ParserDebugCollector
import com.payslipmax.pdfparser.parser.detection.GrammarDiagnosticReport
import com.payslipmax.pdfparser.parser.pipeline.SharedParsingPipeline
import com.payslipmax.pdfparser.parser.registry.DefaultGrammarDescriptors
import com.payslipmax.pdfparser.parser.registry.GrammarRegistry

/**
 * Primary entry facade for Version-Aware Grammar Parsing in PayslipMax.
 * Maintains backwards compatibility while delegating parsing to [SharedParsingPipeline].
 */
object GrammarAwareParser {
    private val registry =
        GrammarRegistry().also {
            DefaultGrammarDescriptors.registerAll(it)
        }
    private val pipeline = SharedParsingPipeline(registry)

    fun parse(
        tokenized: TokenizedPayslip,
        filename: String,
        fallbackExtractor: GemmaFallbackExtractor? = null,
        diagnosticExtractor: GemmaDiagnosticExtractor? = null,
        debugCollector: ParserDebugCollector? = null,
    ): Result<ParsedPayslip> {
        return parseWithDiagnostics(tokenized, filename, fallbackExtractor, diagnosticExtractor, debugCollector).map { it.first }
    }

    fun parseWithDiagnostics(
        tokenized: TokenizedPayslip,
        filename: String,
        fallbackExtractor: GemmaFallbackExtractor? = null,
        diagnosticExtractor: GemmaDiagnosticExtractor? = null,
        debugCollector: ParserDebugCollector? = null,
    ): Result<Pair<ParsedPayslip, GrammarDiagnosticReport>> {
        return pipeline.parse(tokenized, filename, fallbackExtractor, diagnosticExtractor, debugCollector)
    }
}
