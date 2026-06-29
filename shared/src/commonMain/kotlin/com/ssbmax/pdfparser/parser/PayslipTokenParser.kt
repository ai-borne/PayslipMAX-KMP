package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.logging.Logger

/**
 * Phase 4 — the new **primary** parsing entry point. It consumes the platform-independent
 * [TokenizedPayslip] IR (Phase 2) and produces a [ParsedPayslip] entirely in common code via:
 *
 *   tokens → [GridReconstructor] → [RowPairing] → [TokenTableClassifier] → [ReconciliationSolver]
 *
 * There is no `xSplit` crop, no per-month geometry patch, and no hard-fail reconciliation: the grid is
 * reconstructed per-document, the arithmetic invariants drive a confidence score, and genuinely
 * ambiguous fields surface through `fieldConfidence` / `needsReview` for the Phase 5 correction UI.
 *
 * Metadata (date, officer, printed totals) and the income-tax / DSOP pages are still parsed from text,
 * reusing the existing [ParserUtils]/[parseTaxAndSavings] helpers (DRY); the tax pages are flat
 * key→value text where geometry never mattered, so token reconstruction buys nothing there.
 */
object PayslipTokenParser {
    fun parse(
        tokenized: TokenizedPayslip,
        filename: String,
        fallbackExtractor: GemmaFallbackExtractor? = null,
        debugCollector: com.ssbmax.pdfparser.parser.debug.ParserDebugCollector? = null,
    ): Result<ParsedPayslip> {
        return try {
            debugCollector?.recordStage1(tokenized)
            val cleanedFullTextRaw = cleanCommasAndWhitespace(tokenized.fullText)
            val cleanedFullText = negateHindiTransliterations(cleanedFullTextRaw)

            val (monthNum, year) = parseDate(cleanedFullText, filename)
            val monthName = PayslipPatternConfig.monthNames.getOrNull(monthNum) ?: "January"
            val officer = parseOfficer(cleanedFullText, monthNum, year)
            val (grossPay, totalDeductions, netRemittance) = parseTotals(cleanedFullTextRaw)

            val table = TokenTableClassifier.classify(tokenized.tableTokens, debugCollector)
            var solved =
                ReconciliationSolver.solve(
                    table = table,
                    grossPay = grossPay,
                    totalDeductions = totalDeductions,
                    netRemittance = netRemittance,
                    fullText = tokenized.fullText,
                    filename = filename,
                    debugCollector = debugCollector,
                )

            if (fallbackExtractor != null && (solved.needsReview || solved.rawEarnings.isNotEmpty() || solved.rawDeductions.isNotEmpty())) {
                solved = applyGemmaFallback(solved, fallbackExtractor)
            }

            Logger.d("PayslipTokenParser", "filename: $filename, gross: $grossPay, deductions: $totalDeductions, net: $netRemittance, needsReview: ${solved.needsReview}")

            val taxText = TokenText.readingOrder(tokenized.taxTokens)
            val dsopText = TokenText.readingOrder(tokenized.dsopTokens)
            val taxAndSavings = parseTaxAndSavings(taxText, dsopText, cleanedFullText)
            val dateStr = "${monthNum.toString().padStart(2, '0')}/$year"

            val parsed =
                assembleParsedPayslip(
                    filename = filename, year = year, monthNum = monthNum, monthName = monthName,
                    dateStr = dateStr, officer = officer, earningsMap = solved.earningsMap,
                    deductionsMap = solved.deductionsMap, reconciled = solved.reconciled,
                    taxAndSavings = taxAndSavings, rawEarnings = solved.rawEarnings, rawDeductions = solved.rawDeductions,
                )

            val schemaValidation =
                SchemaValidator.validate(
                    grossPay = grossPay,
                    totalDeductions = totalDeductions,
                    netRemittance = netRemittance,
                    creditsSum = solved.earningsMap.values.sum() + solved.rawEarnings.values.sum(),
                    debitsSum = solved.deductionsMap.values.sum() + solved.rawDeductions.values.sum(),
                )

            val finalParsed = parsed.copy(fieldConfidence = solved.fieldConfidence, needsReview = solved.needsReview || !schemaValidation.isValid)

            val missingCredits = PayslipPatternConfig.strictlyMandatoryCredits.filter { (solved.earningsMap[it] ?: 0.0) <= 0.0 }
            val missingDebits = PayslipPatternConfig.strictlyMandatoryDebits.filter { (solved.deductionsMap[it] ?: 0.0) <= 0.0 }
            val missingMandatory = missingCredits + missingDebits

            println("=== STAGE 3: FINAL PARSER MODEL ===")
            println("earningsMap: ${finalParsed.earnings}")
            println("deductionsMap: ${finalParsed.deductions}")
            println("rawEarnings: ${finalParsed.rawEarnings}")
            println("rawDeductions: ${finalParsed.rawDeductions}")
            println("summary: grossPay=$grossPay, totalDeductions=$totalDeductions, netRemittance=$netRemittance")
            println("missingMandatoryFields: $missingMandatory")
            println("fieldConfidence: ${finalParsed.fieldConfidence}")
            println("needsReview: ${finalParsed.needsReview}")
            println("===================================")

            Result.success(finalParsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun applyGemmaFallback(
        solved: SolvedTable,
        extractor: GemmaFallbackExtractor,
    ): SolvedTable {
        return try {
            val extractions =
                kotlinx.coroutines.runBlocking {
                    extractor.extractFallback(solved.rawEarnings, solved.rawDeductions)
                }
            if (extractions.earnings.isEmpty() && extractions.deductions.isEmpty()) return solved

            val mergedEarnings = solved.earningsMap.toMutableMap()
            val mergedDeductions = solved.deductionsMap.toMutableMap()
            extractions.earnings.forEach { (k, v) -> mergedEarnings[k] = (mergedEarnings[k] ?: 0.0) + v }
            extractions.deductions.forEach { (k, v) -> mergedDeductions[k] = (mergedDeductions[k] ?: 0.0) + v }

            solved.copy(earningsMap = mergedEarnings, deductionsMap = mergedDeductions)
        } catch (e: Exception) {
            solved
        }
    }
}
