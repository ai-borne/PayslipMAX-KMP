package com.ssbmax.pdfparser.parser.pipeline

import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.domain.TaxAndSavings
import com.ssbmax.pdfparser.logging.Logger
import com.ssbmax.pdfparser.parser.GemmaFallbackExtractor
import com.ssbmax.pdfparser.parser.PayslipPatternConfig
import com.ssbmax.pdfparser.parser.ReconciliationSolver
import com.ssbmax.pdfparser.parser.SchemaValidator
import com.ssbmax.pdfparser.parser.SolvedTable
import com.ssbmax.pdfparser.parser.TokenTableClassifier
import com.ssbmax.pdfparser.parser.TokenText
import com.ssbmax.pdfparser.parser.TokenizedPayslip
import com.ssbmax.pdfparser.parser.applyGemmaFallback
import com.ssbmax.pdfparser.parser.assembleParsedPayslip
import com.ssbmax.pdfparser.parser.cleanCommasAndWhitespace
import com.ssbmax.pdfparser.parser.debug.ParserDebugCollector
import com.ssbmax.pdfparser.parser.detection.GrammarDiagnosticReport
import com.ssbmax.pdfparser.parser.negateHindiTransliterations
import com.ssbmax.pdfparser.parser.parseDate
import com.ssbmax.pdfparser.parser.parseOfficer
import com.ssbmax.pdfparser.parser.parseTaxAndSavings
import com.ssbmax.pdfparser.parser.parseTotals
import com.ssbmax.pdfparser.parser.registry.GrammarDescriptor
import com.ssbmax.pdfparser.parser.registry.GrammarRegistry

/**
 * Orchestrator implementing the shared 7-step parsing pipeline.
 * Integrates grammar detection, strategy delegation, and core financial reconciliation engines.
 */
class SharedParsingPipeline(
    private val registry: GrammarRegistry,
) {
    fun parse(
        tokenized: TokenizedPayslip,
        filename: String,
        fallbackExtractor: GemmaFallbackExtractor? = null,
        debugCollector: ParserDebugCollector? = null,
    ): Result<Pair<ParsedPayslip, GrammarDiagnosticReport>> {
        return try {
            debugCollector?.recordStage1(tokenized)
            val initialContext = createInitialContext(tokenized, filename)
            val (descriptor, report) = registry.detectAndSelect(tokenized)

            val officer = extractOfficer(descriptor, initialContext)
            val solved = executeTableReconciliation(descriptor, initialContext, fallbackExtractor, debugCollector)
            val taxAndSavings = extractTaxAndSavings(descriptor, initialContext)

            val finalParsed = assembleAndValidate(initialContext, officer, solved, taxAndSavings)
            val finalReport =
                report.copy(
                    validationStatus = if (finalParsed.needsReview) "NEEDS_REVIEW" else "PASSED",
                )

            Logger.d(TAG, "filename: $filename, gross: ${initialContext.grossPay}, net: ${initialContext.netRemittance}, grammar: ${finalReport.selectedFamily}")
            Result.success(Pair(finalParsed, finalReport))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createInitialContext(
        tokenized: TokenizedPayslip,
        filename: String,
    ): PipelineContext {
        val cleanedFullTextRaw = cleanCommasAndWhitespace(tokenized.fullText)
        val cleanedFullText = negateHindiTransliterations(cleanedFullTextRaw)
        val (monthNum, year) = parseDate(cleanedFullText, filename)
        val monthName = PayslipPatternConfig.monthNames.getOrNull(monthNum) ?: "January"
        val (grossPay, totalDeductions, netRemittance) = parseTotals(cleanedFullTextRaw)
        val dateStr = "${monthNum.toString().padStart(2, '0')}/$year"

        return PipelineContext(
            tokenized = tokenized,
            filename = filename,
            cleanedFullTextRaw = cleanedFullTextRaw,
            cleanedFullText = cleanedFullText,
            monthNum = monthNum,
            year = year,
            monthName = monthName,
            dateStr = dateStr,
            grossPay = grossPay,
            totalDeductions = totalDeductions,
            netRemittance = netRemittance,
        )
    }

    private fun extractOfficer(
        descriptor: GrammarDescriptor?,
        context: PipelineContext,
    ): Officer {
        return descriptor?.strategySet?.headerStrategy?.extractOfficer(context.tokenized, context.cleanedFullText)
            ?: parseOfficer(context.cleanedFullText, context.monthNum, context.year)
    }

    private fun executeTableReconciliation(
        descriptor: GrammarDescriptor?,
        context: PipelineContext,
        fallbackExtractor: GemmaFallbackExtractor?,
        debugCollector: ParserDebugCollector?,
    ): SolvedTable {
        val table = TokenTableClassifier.classify(context.tokenized.tableTokens, debugCollector)
        var solved =
            ReconciliationSolver.solve(
                table = table,
                grossPay = context.grossPay,
                totalDeductions = context.totalDeductions,
                netRemittance = context.netRemittance,
                fullText = context.tokenized.fullText,
                filename = context.filename,
                debugCollector = debugCollector,
            )

        if (fallbackExtractor != null && (solved.needsReview || solved.rawEarnings.isNotEmpty() || solved.rawDeductions.isNotEmpty())) {
            solved = applyGemmaFallback(solved, fallbackExtractor)
        }
        return solved
    }

    private fun extractTaxAndSavings(
        descriptor: GrammarDescriptor?,
        context: PipelineContext,
    ): TaxAndSavings? {
        val strategyTax = descriptor?.strategySet?.pageStrategy?.extractTaxAndSavings(context.tokenized, context.cleanedFullText)
        if (strategyTax != null) return strategyTax

        val taxText = TokenText.readingOrder(context.tokenized.taxTokens)
        val dsopText = TokenText.readingOrder(context.tokenized.dsopTokens)
        return parseTaxAndSavings(taxText, dsopText, context.cleanedFullText)
    }

    private fun assembleAndValidate(
        context: PipelineContext,
        officer: Officer,
        solved: SolvedTable,
        taxAndSavings: TaxAndSavings?,
    ): ParsedPayslip {
        val parsed =
            assembleParsedPayslip(
                filename = context.filename, year = context.year, monthNum = context.monthNum, monthName = context.monthName,
                dateStr = context.dateStr, officer = officer, earningsMap = solved.earningsMap,
                deductionsMap = solved.deductionsMap, reconciled = solved.reconciled,
                taxAndSavings = taxAndSavings, rawEarnings = solved.rawEarnings, rawDeductions = solved.rawDeductions,
            )

        val schemaValidation =
            SchemaValidator.validate(
                grossPay = context.grossPay,
                totalDeductions = context.totalDeductions,
                netRemittance = context.netRemittance,
                creditsSum = solved.earningsMap.values.sum() + solved.rawEarnings.values.sum(),
                debitsSum = solved.deductionsMap.values.sum() + solved.rawDeductions.values.sum(),
            )

        return parsed.copy(
            fieldConfidence = solved.fieldConfidence,
            fieldSource = solved.fieldSource,
            needsReview = solved.needsReview || !schemaValidation.isValid,
        )
    }

    companion object {
        private const val TAG = "SharedParsingPipeline"
    }
}
