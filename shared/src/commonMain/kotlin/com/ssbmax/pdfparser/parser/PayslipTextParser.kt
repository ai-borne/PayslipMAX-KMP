package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.*
import com.ssbmax.pdfparser.logging.Logger

object PayslipTextParser {
    fun parse(
        flatText: String,
        filename: String,
    ): Result<ParsedPayslip> {
        return parse(
            leftColumnText = flatText,
            middleColumnText = flatText,
            fullText = flatText,
            filename = filename,
        )
    }

    fun parse(
        leftColumnText: String,
        middleColumnText: String,
        fullText: String,
        taxPageText: String? = null,
        dsopPageText: String? = null,
        filename: String,
    ): Result<ParsedPayslip> {
        return try {
            val cleanedFullTextRaw = cleanCommasAndWhitespace(fullText)
            val cleanedFullText = negateHindiTransliterations(cleanedFullTextRaw)
            val cleanedLeftText = negateHindiTransliterations(cleanCommasAndWhitespace(stripNotesAndDescriptions(leftColumnText)))
            val cleanedMiddleText = negateHindiTransliterations(cleanCommasAndWhitespace(stripNotesAndDescriptions(middleColumnText)))

            val (monthNum, year) = parseDate(cleanedFullText, filename)
            val monthName = PayslipPatternConfig.monthNames.getOrNull(monthNum) ?: "January"

            val officer = parseOfficer(cleanedFullText, monthNum, year)
            val (grossPay, totalDeductions, netRemittance) = parseTotals(cleanedFullTextRaw)
            Logger.d("PayslipTextParser", "filename: $filename, grossPay: $grossPay, totalDeductions: $totalDeductions, netRemittance: $netRemittance")

            // Extract Earnings (Left Column) & Deductions (Middle Column)
            val leftExtracted =
                extractFromColumn(cleanedLeftText, PayslipPatternConfig.creditKeysMapping, PayslipPatternConfig.debitKeysMapping)
            val middleExtracted =
                extractFromColumn(cleanedMiddleText, PayslipPatternConfig.creditKeysMapping, PayslipPatternConfig.debitKeysMapping)

            val hasBpayInFull = cleanedFullText.lowercase().contains("basic pay") || cleanedFullText.lowercase().contains("bpay")

            var finalLeftExtracted = leftExtracted
            var finalMiddleExtracted = middleExtracted
            var isSplit = leftColumnText != middleColumnText

            val hasBpayInSplit = leftExtracted.keys.any { PayslipPatternConfig.creditKeysMapping[it] == "basicPay" }

            if ((!isSplit || !hasBpayInSplit) && hasBpayInFull) {
                // Column crop did not work or failed to capture basic pay numbers.
                // Split at the first debit-only anchor (DSOPF/AGIF) of the full text to get credit and debit sections.
                val (creditSectionText, debitSectionText, anchorFound) = splitCreditDebitSections(cleanedFullText)
                if (anchorFound) {
                    finalLeftExtracted = extractFromColumn(creditSectionText, PayslipPatternConfig.creditKeysMapping, PayslipPatternConfig.debitKeysMapping)
                    finalMiddleExtracted = extractFromColumn(debitSectionText, PayslipPatternConfig.creditKeysMapping, PayslipPatternConfig.debitKeysMapping)
                    // Treat as split so debit-keys in credit section go to adjPayAndAllce
                    isSplit = true
                } else {
                    // No debit anchor found — fall back to full-text extraction without split semantics.
                    finalLeftExtracted = extractFromColumn(cleanedFullText, PayslipPatternConfig.creditKeysMapping, PayslipPatternConfig.debitKeysMapping)
                    finalMiddleExtracted = extractFromColumn(cleanedFullText, PayslipPatternConfig.creditKeysMapping, PayslipPatternConfig.debitKeysMapping)
                    // isSplit remains false
                }
            }

            val cleanedLeftTextPreserved = cleanPreservingNewlines(stripNotesAndDescriptions(leftColumnText))
            val cleanedMiddleTextPreserved = cleanPreservingNewlines(stripNotesAndDescriptions(middleColumnText))
            val cleanedFullTextPreserved = cleanPreservingNewlines(fullText)

            val (dynamicEarnings, dynamicDeductions) =
                DynamicSpatialParser.extractDynamicEarningsAndDeductions(
                    isSplit = isSplit,
                    leftColumnText = leftColumnText,
                    middleColumnText = middleColumnText,
                    fullText = fullText,
                    cleanedLeftText = cleanedLeftTextPreserved,
                    cleanedMiddleText = cleanedMiddleTextPreserved,
                    cleanedFullText = cleanedFullTextPreserved,
                )

            val earningsMap = mutableMapOf<String, Double>()
            val deductionsMap = mutableMapOf<String, Double>()

            // Cross-column routing key-sets are the SSOT in PayslipPatternConfig (shared with the
            // Phase 4 ReconciliationSolver) so both pipelines book ledger entries and reversals alike.
            val ledgerDebitKeys = PayslipPatternConfig.ledgerDebitKeys
            val creditReversalDebitKeys = PayslipPatternConfig.creditReversalDebitKeys

            Logger.d("PayslipTextParser", "filename: $filename")
            Logger.d("PayslipTextParser", "cleanedLeftText: '$cleanedLeftText'")
            Logger.d("PayslipTextParser", "cleanedLeftText CHAR CODES: ${cleanedLeftText.map { it.code }.joinToString(",")}")
            Logger.d("PayslipTextParser", "cleanedMiddleText: '$cleanedMiddleText'")
            Logger.d("PayslipTextParser", "cleanedMiddleText CHAR CODES: ${cleanedMiddleText.map { it.code }.joinToString(",")}")
            Logger.d("PayslipTextParser", "finalLeftExtracted: $finalLeftExtracted")
            Logger.d("PayslipTextParser", "finalMiddleExtracted: $finalMiddleExtracted")

            for ((key, value) in finalLeftExtracted) {
                if (key in PayslipPatternConfig.creditKeysMapping.keys) {
                    val stdKey = PayslipPatternConfig.creditKeysMapping[key]!!
                    earningsMap[stdKey] = (earningsMap[stdKey] ?: 0.0) + value
                } else if (isSplit && key in PayslipPatternConfig.debitKeysMapping.keys) {
                    val baseStdKey = PayslipPatternConfig.debitKeysMapping[key]!!
                    if (baseStdKey in ledgerDebitKeys) {
                        // Route ledger entries to deductionsMap so they can be removed correctly later
                        deductionsMap[baseStdKey] = (deductionsMap[baseStdKey] ?: 0.0) + value
                    } else if (baseStdKey in creditReversalDebitKeys) {
                        // This is a credit reversal of a deduction — add to earnings adjustments
                        earningsMap["adjPayAndAllce"] = (earningsMap["adjPayAndAllce"] ?: 0.0) + value
                    }
                    // Other debit keys in credit section (e.g. dsopSubscription, agif, incomeTax)
                    // are ignored — they should not appear in credit section; if they do, skip them.
                }
            }

            for ((key, value) in finalMiddleExtracted) {
                if (key in PayslipPatternConfig.debitKeysMapping.keys) {
                    val stdKey = PayslipPatternConfig.debitKeysMapping[key]!!
                    deductionsMap[stdKey] = (deductionsMap[stdKey] ?: 0.0) + value
                } else if (isSplit && key in PayslipPatternConfig.creditKeysMapping.keys) {
                    val baseStdKey = PayslipPatternConfig.creditKeysMapping[key]!!
                    val targetKey = PayslipPatternConfig.recoveryTargetFor(baseStdKey)
                    deductionsMap[targetKey] = (deductionsMap[targetKey] ?: 0.0) + value
                }
            }

            val reconciled =
                reconcileTotals(
                    earningsMap = earningsMap,
                    deductionsMap = deductionsMap,
                    fullText = fullText,
                    grossPay = grossPay,
                    totalDeductions = totalDeductions,
                    netRemittance = netRemittance,
                    filename = filename,
                )

            val taxAndSavings = parseTaxAndSavings(taxPageText, dsopPageText, cleanedFullText)
            val dateStr = "${monthNum.toString().padStart(2, '0')}/$year"

            Result.success(
                assembleParsedPayslip(
                    filename = filename,
                    year = year,
                    monthNum = monthNum,
                    monthName = monthName,
                    dateStr = dateStr,
                    officer = officer,
                    earningsMap = earningsMap,
                    deductionsMap = deductionsMap,
                    reconciled = reconciled,
                    taxAndSavings = taxAndSavings,
                    rawEarnings = dynamicEarnings,
                    rawDeductions = dynamicDeductions,
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
