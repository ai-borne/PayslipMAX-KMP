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

            val (dynamicEarnings, dynamicDeductions) = DynamicSpatialParser.extractDynamicEarningsAndDeductions(
                isSplit = isSplit,
                leftColumnText = leftColumnText,
                middleColumnText = middleColumnText,
                fullText = fullText,
                cleanedLeftText = cleanedLeftText,
                cleanedMiddleText = cleanedMiddleText,
                cleanedFullText = cleanedFullText
            )

            val earningsMap = mutableMapOf<String, Double>()
            val deductionsMap = mutableMapOf<String, Double>()

            // Debit keys that represent ledger balance entries — they must never go to adjPayAndAllce.
            // They are handled separately by the earningsMap/deductionsMap.remove() calls below.
            val ledgerDebitKeys = setOf("openingDebitBalance", "closingCreditBalance")
            val creditReversalDebitKeys = setOf("licenseFee", "furnitureRent", "waterCharges", "electricityCharges", "barrackDamage", "ticketRecovery")

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
                    val stdKey = "rec" + baseStdKey.replaceFirstChar { it.uppercaseChar() }
                    val targetKey =
                        when (stdKey) {
                            "recFieldAllowance" -> "recFieldAllowance"
                            "recSpecialForces", "recSpecialForcesPay" -> "recSpecialForces"
                            else -> "recoveryOfDebits"
                        }
                    deductionsMap[targetKey] = (deductionsMap[targetKey] ?: 0.0) + value
                }
            }

            DynamicSpatialParser.applyHistoricalOverrides(year, monthNum, earningsMap, deductionsMap)

            val openingCr = earningsMap.remove("openingCreditBalance") ?: 0.0
            val closingDr = earningsMap.remove("closingDebitBalance") ?: 0.0
            val openingDr = deductionsMap.remove("openingDebitBalance") ?: 0.0
            val closingCr = deductionsMap.remove("closingCreditBalance") ?: 0.0

            val sumEarnings = earningsMap.values.sum()
            val sumDeductions = deductionsMap.values.sum()

            var realGross = grossPay
            if (realGross == 0.0) {
                realGross = sumEarnings
            }

            var realDeductions = totalDeductions
            if (realDeductions == 0.0 ||
                realDeductions == realGross ||
                realDeductions == netRemittance
            ) {
                realDeductions = sumDeductions
            }

            val finalNet =
                if (netRemittance == 0.0) {
                    realGross - realDeductions
                } else {
                    netRemittance
                }

            // Subtract carried-over ledger balances from printed totals for true reconciliation math
            val trueGross = (realGross - openingCr - closingDr).coerceAtLeast(0.0)
            val trueDeductions = (realDeductions - openingDr - closingCr).coerceAtLeast(0.0)

            if (netRemittance != 0.0) {
                val expectedNet = if (realDeductions == sumDeductions) {
                    realGross - realDeductions - openingDr - closingCr
                } else {
                    realGross - realDeductions
                }
                val reconciliationDiff = kotlin.math.abs(expectedNet - finalNet)
                if (reconciliationDiff >= 2.0) {
                    return Result.failure(Exception("Mathematical reconciliation check failed for $filename. expectedNet: $expectedNet, finalNet: $finalNet, Diff: $reconciliationDiff"))
                }
            }

            val miscCr =
                if (trueGross > 0.0 && trueGross > sumEarnings) {
                    trueGross - sumEarnings
                } else {
                    if (sumEarnings > trueGross && trueGross > 0.0) {
                        Logger.w("PayslipTextParser", "Parsed sum of earnings ($sumEarnings) exceeds true gross pay ($trueGross) in $filename")
                    }
                    0.0
                }

            val miscDr =
                if (trueDeductions > 0.0 && trueDeductions > sumDeductions) {
                    trueDeductions - sumDeductions
                } else {
                    if (sumDeductions > trueDeductions && trueDeductions > 0.0) {
                        Logger.w("PayslipTextParser", "Parsed sum of deductions ($sumDeductions) exceeds true total deductions ($trueDeductions) in $filename")
                    }
                    0.0
                }

            val earnings =
                Earnings(
                    basicPay = earningsMap["basicPay"] ?: 0.0,
                    dearnessAllowance = earningsMap["dearnessAllowance"] ?: 0.0,
                    militaryServicePay = earningsMap["militaryServicePay"] ?: 0.0,
                    transportAllowance = earningsMap["transportAllowance"] ?: 0.0,
                    transportAllowanceDa = earningsMap["transportAllowanceDa"] ?: 0.0,
                    dressAllowance = earningsMap["dressAllowance"] ?: 0.0,
                    rationMoney = earningsMap["rationMoney"] ?: 0.0,
                    specialForcesPay = earningsMap["specialForcesPay"] ?: 0.0,
                    fieldAllowance = earningsMap["fieldAllowance"] ?: 0.0,
                    childrenEducationAllowance = earningsMap["childrenEducationAllowance"] ?: 0.0,
                    houseRentAllowance = earningsMap["houseRentAllowance"] ?: 0.0,
                    riskHardshipAllowance = earningsMap["riskHardshipAllowance"] ?: 0.0,
                    nonPracticingAllowance = earningsMap["nonPracticingAllowance"] ?: 0.0,
                    adjBasicPay = earningsMap["adjBasicPay"] ?: 0.0,
                    adjDa = earningsMap["adjDa"] ?: 0.0,
                    adjMsp = earningsMap["adjMsp"] ?: 0.0,
                    adjTpta = earningsMap["adjTpta"] ?: 0.0,
                    arrearsCea = earningsMap["arrearsCea"] ?: 0.0,
                    arrearsDa = earningsMap["arrearsDa"] ?: 0.0,
                    arrearsRation = earningsMap["arrearsRation"] ?: 0.0,
                    arrearsSpecialForces = earningsMap["arrearsSpecialForces"] ?: 0.0,
                    arrearsTpta = earningsMap["arrearsTpta"] ?: 0.0,
                    arrearsTptaDa = earningsMap["arrearsTptaDa"] ?: 0.0,
                    arrearsHra = earningsMap["arrearsHra"] ?: 0.0,
                    arrearsRiskHardship = earningsMap["arrearsRiskHardship"] ?: 0.0,
                    adjPayAndAllce = earningsMap["adjPayAndAllce"] ?: 0.0,
                    adjFieldAllowance = earningsMap["adjFieldAllowance"] ?: 0.0,
                    medicalAllowance = earningsMap["medicalAllowance"] ?: 0.0,
                    adjTicketRecovery = earningsMap["adjTicketRecovery"] ?: 0.0,
                    miscEarnings = miscCr,
                )

            val deductions =
                Deductions(
                    dsopSubscription = deductionsMap["dsopSubscription"] ?: 0.0,
                    agif = deductionsMap["agif"] ?: 0.0,
                    incomeTax = deductionsMap["incomeTax"] ?: 0.0,
                    educationCess = deductionsMap["educationCess"] ?: 0.0,
                    licenseFee = deductionsMap["licenseFee"] ?: 0.0,
                    furnitureRent = deductionsMap["furnitureRent"] ?: 0.0,
                    waterCharges = deductionsMap["waterCharges"] ?: 0.0,
                    electricityCharges = deductionsMap["electricityCharges"] ?: 0.0,
                    barrackDamage = deductionsMap["barrackDamage"] ?: 0.0,
                    ticketRecovery = deductionsMap["ticketRecovery"] ?: 0.0,
                    recFieldAllowance = deductionsMap["recFieldAllowance"] ?: 0.0,
                    recSpecialForces = deductionsMap["recSpecialForces"] ?: 0.0,
                    recoveryOfDebits = deductionsMap["recoveryOfDebits"] ?: 0.0,
                    aobf = deductionsMap["aobf"] ?: 0.0,
                    agifLoanRecovery = deductionsMap["agifLoanRecovery"] ?: 0.0,
                    miscDeductions = miscDr,
                )

            val ledgerBalances =
                LedgerBalances(
                    openingCreditBalance = openingCr,
                    openingDebitBalance = openingDr,
                    closingCreditBalance = closingCr,
                    closingDebitBalance = closingDr,
                )

            val summary = PayslipSummary(grossPay = realGross, totalDeductions = realDeductions, netRemittance = finalNet)
            val taxAndSavings = parseTaxAndSavings(taxPageText, dsopPageText, cleanedFullText)
            val dateStr = "${monthNum.toString().padStart(2, '0')}/$year"

            Result.success(
                ParsedPayslip(
                    file = filename,
                    year = year,
                    monthNum = monthNum,
                    monthName = monthName,
                    dateStr = dateStr,
                    officer = officer,
                    earnings = earnings,
                    deductions = deductions,
                    ledgerBalances = ledgerBalances,
                    summary = summary,
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
