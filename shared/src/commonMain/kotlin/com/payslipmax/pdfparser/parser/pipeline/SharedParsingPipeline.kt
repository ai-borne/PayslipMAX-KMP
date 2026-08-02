package com.payslipmax.pdfparser.parser.pipeline

import com.payslipmax.pdfparser.domain.DiagnosticSuggestion
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.logging.Logger
import com.payslipmax.pdfparser.parser.GemmaDiagnosticExtractor
import com.payslipmax.pdfparser.parser.GemmaFallbackExtractor
import com.payslipmax.pdfparser.parser.PayslipPatternConfig
import com.payslipmax.pdfparser.parser.ReconciliationSolver
import com.payslipmax.pdfparser.parser.SchemaValidationResult
import com.payslipmax.pdfparser.parser.SchemaValidator
import com.payslipmax.pdfparser.parser.SolvedTable
import com.payslipmax.pdfparser.parser.TokenTableClassifier
import com.payslipmax.pdfparser.parser.TokenText
import com.payslipmax.pdfparser.parser.TokenizedPayslip
import com.payslipmax.pdfparser.parser.applyGemmaFallback
import com.payslipmax.pdfparser.parser.assembleParsedPayslip
import com.payslipmax.pdfparser.parser.cleanCommasAndWhitespace
import com.payslipmax.pdfparser.parser.debug.ParserDebugCollector
import com.payslipmax.pdfparser.parser.detection.GrammarDiagnosticReport
import com.payslipmax.pdfparser.parser.negateHindiTransliterations
import com.payslipmax.pdfparser.parser.parseDate
import com.payslipmax.pdfparser.parser.parseOfficer
import com.payslipmax.pdfparser.parser.parseTaxAndSavings
import com.payslipmax.pdfparser.parser.parseTotals
import com.payslipmax.pdfparser.parser.registry.GrammarDescriptor
import com.payslipmax.pdfparser.parser.registry.GrammarRegistry

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
        diagnosticExtractor: GemmaDiagnosticExtractor? = null,
        debugCollector: ParserDebugCollector? = null,
    ): Result<Pair<ParsedPayslip, GrammarDiagnosticReport>> {
        return try {
            debugCollector?.recordStage1(tokenized)
            val preFlight = com.payslipmax.pdfparser.parser.validation.PdfPreFlightValidator.validate(tokenized)
            if (preFlight is com.payslipmax.pdfparser.parser.validation.PdfPreFlightResult.Invalid) {
                return Result.failure(IllegalArgumentException("PdfPreFlightValidationFailed: ${preFlight.reason}"))
            }
            val initialContext = createInitialContext(tokenized, filename)
            val (descriptor, report) = registry.detectAndSelect(tokenized)

            val officer = extractOfficer(descriptor, initialContext)
            val solved = executeTableReconciliation(descriptor, initialContext, fallbackExtractor, debugCollector)
            val taxAndSavings = extractTaxAndSavings(descriptor, initialContext)

            val finalParsed = assembleAndValidate(initialContext, officer, solved, taxAndSavings, diagnosticExtractor)
            val finalReport =
                report.copy(
                    validationStatus = if (finalParsed.needsReview) "NEEDS_REVIEW" else "PASSED",
                )

            Logger.d(
                TAG,
                "filename: $filename, gross: ${initialContext.grossPay}, net: ${initialContext.netRemittance}, " +
                    "grammar: ${finalReport.selectedFamily}, needsReview: ${finalParsed.needsReview}, " +
                    "diagnostic: ${finalParsed.diagnosticSuggestion?.fieldKey ?: "none"}",
            )
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
        val table =
            TokenTableClassifier.classify(
                context.tokenized.tableTokens,
                debugCollector,
                context.grossPay,
                context.totalDeductions,
            )
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
        diagnosticExtractor: GemmaDiagnosticExtractor?,
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

        val reviewReasons =
            if (schemaValidation.isValid) {
                solved.reviewReasons
            } else {
                solved.reviewReasons +
                    "Schema validation failed (post Tier 6): gross mismatch ${schemaValidation.grossMismatch}, " +
                    "deductions mismatch ${schemaValidation.deductionsMismatch}, net residual ${schemaValidation.netResidual}"
            }

        val diagnosticSuggestion =
            if (!schemaValidation.isValid && diagnosticExtractor != null) {
                runDiagnostic(diagnosticExtractor, solved, context, schemaValidation)
            } else {
                null
            }

        return parsed.copy(
            fieldConfidence = solved.fieldConfidence,
            fieldSource = solved.fieldSource,
            needsReview = solved.needsReview || !schemaValidation.isValid,
            reviewReasons = reviewReasons,
            diagnosticSuggestion = diagnosticSuggestion,
        )
    }

    private fun runDiagnostic(
        diagnosticExtractor: GemmaDiagnosticExtractor,
        solved: SolvedTable,
        context: PipelineContext,
        schemaValidation: SchemaValidationResult,
    ): DiagnosticSuggestion? {
        return try {
            kotlinx.coroutines.runBlocking {
                diagnosticExtractor.suggestDiagnostic(
                    earnings = solved.earningsMap,
                    deductions = solved.deductionsMap,
                    grossPay = context.grossPay,
                    totalDeductions = context.totalDeductions,
                    netRemittance = context.netRemittance,
                    residual = maxOf(schemaValidation.grossMismatch, schemaValidation.deductionsMismatch, schemaValidation.netResidual),
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "SharedParsingPipeline"
    }
}
