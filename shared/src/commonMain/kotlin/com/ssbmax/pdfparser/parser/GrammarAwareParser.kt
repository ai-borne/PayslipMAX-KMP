package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.parser.debug.ParserDebugCollector
import com.ssbmax.pdfparser.parser.detection.GrammarDiagnosticReport
import com.ssbmax.pdfparser.parser.pipeline.SharedParsingPipeline
import com.ssbmax.pdfparser.parser.registry.DefaultGrammarDescriptors
import com.ssbmax.pdfparser.parser.registry.GrammarRegistry

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
        debugCollector: ParserDebugCollector? = null,
    ): Result<ParsedPayslip> {
        return parseWithDiagnostics(tokenized, filename, fallbackExtractor, debugCollector).map { it.first }
    }

    fun parseWithDiagnostics(
        tokenized: TokenizedPayslip,
        filename: String,
        fallbackExtractor: GemmaFallbackExtractor? = null,
        debugCollector: ParserDebugCollector? = null,
    ): Result<Pair<ParsedPayslip, GrammarDiagnosticReport>> {
        return pipeline.parse(tokenized, filename, fallbackExtractor, debugCollector)
    }
}
