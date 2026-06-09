package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.*

object PayslipTextParser {

    fun parse(flatText: String, filename: String): Result<ParsedPayslip> {
        return parse(
            leftColumnText = flatText,
            middleColumnText = flatText,
            fullText = flatText,
            filename = filename
        )
    }

    fun parse(
        leftColumnText: String,
        middleColumnText: String,
        fullText: String,
        taxPageText: String? = null,
        dsopPageText: String? = null,
        filename: String
    ): Result<ParsedPayslip> {
        return try {
            val cleanedFullText = cleanCommasAndWhitespace(fullText)
            val cleanedLeftText = cleanCommasAndWhitespace(leftColumnText)
            val cleanedMiddleText = cleanCommasAndWhitespace(middleColumnText)

            // Extract month and year from text, or fallback to filename
            val dateMatch = Regex("STATEMENT OF ACCOUNT FOR (\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE).find(cleanedFullText)
            val (monthNum, year) = if (dateMatch != null) {
                Pair(
                    dateMatch.groupValues[1].toIntOrNull() ?: 1,
                    dateMatch.groupValues[2].toIntOrNull() ?: 2024
                )
            } else {
                val fileMonthMatch = Regex("\\d+\\s+([a-zA-Z]+)", RegexOption.IGNORE_CASE).find(filename)
                val fileYearMatch = Regex("(\\d{4})").find(filename)
                val mNum = fileMonthMatch?.groupValues?.get(1)?.lowercase()?.let { PayslipPatternConfig.monthMap[it] } ?: 1
                val yVal = fileYearMatch?.groupValues?.get(1)?.toIntOrNull() ?: 2024
                Pair(mNum, yVal)
            }
            val monthName = PayslipPatternConfig.monthNames.getOrNull(monthNum) ?: "January"

            // Extract Name, CDA A/C No, PAN
            val nameRegex = Regex("(?:Name|naama/Name)\\s*:\\s*([A-Za-z\\s]+)", RegexOption.IGNORE_CASE)
            val acRegex = Regex("(?:A/C No|CDA A/C NO|laoKa saM#yaa /A/C No)\\s*[:\\-–]?\\s*([^\\s]+)", RegexOption.IGNORE_CASE)
            val panRegex = Regex("(?:PAN No|sqaayaI Kata saM#yaa/PAN No)\\s*:\\s*([^\\s]+)", RegexOption.IGNORE_CASE)

            var officerName = nameRegex.find(cleanedFullText)?.groupValues?.get(1)?.trim() ?: "Officer Officer Officer"
            officerName = officerName.split(Regex("A/C|Email|PAN|Basic|BPAY|CDA", RegexOption.IGNORE_CASE))[0].trim()
            if (officerName.endsWith(" A", ignoreCase = true)) {
                officerName = officerName.substring(0, officerName.length - 2).trim()
            }

            var accountNo = acRegex.find(cleanedFullText)?.groupValues?.get(1)?.trim() ?: "16/000/000000X"
            if (accountNo.endsWith("PAN")) {
                accountNo = accountNo.removeSuffix("PAN").trim()
            }
            if (accountNo.startsWith(":")) {
                accountNo = accountNo.removePrefix(":").trim()
            }

            val panNo = panRegex.find(cleanedFullText)?.groupValues?.get(1)?.trim() ?: "AR*****90G"

            // Extract Totals
            val totalsMapping = mapOf(
                "Gross Pay" to listOf("kula Aaya Gross Pay", "Gross Pay", "Total Credit"),
                "Total Deductions" to listOf("kula kTaOtI Total Deductions", "Total Deductions", "Total Debit"),
                "Net Remittance" to listOf("Net Remittance", "REMITTANCE", "inavala p`oiYat Qana/Net Remittance")
            )

            val extractedTotals = mutableMapOf<String, Double>()
            for ((term, keys) in totalsMapping) {
                for (key in keys) {
                    val escapedKey = Regex.escape(key)
                    val pattern = Regex("(?<![a-zA-Z0-9])$escapedKey(?![a-zA-Z0-9])\\s*[:\\-–]?\\s*(?:Rs\\.?\\s*)?(\\d+)", RegexOption.IGNORE_CASE)
                    val match = pattern.find(cleanedFullText)
                    if (match != null) {
                        extractedTotals[term] = match.groupValues[1].toDoubleOrNull() ?: 0.0
                        break
                    }
                }
            }

            val grossPay = extractedTotals["Gross Pay"] ?: 0.0
            val totalDeductions = extractedTotals["Total Deductions"] ?: 0.0
            val netRemittance = extractedTotals["Net Remittance"] ?: 0.0

            // Extract Earnings (Left Column) & Deductions (Middle Column)
            val leftExtracted = extractFromColumn(cleanedLeftText, PayslipPatternConfig.creditKeysMapping, PayslipPatternConfig.debitKeysMapping)
            val middleExtracted = extractFromColumn(cleanedMiddleText, PayslipPatternConfig.creditKeysMapping, PayslipPatternConfig.debitKeysMapping)

            val earningsMap = mutableMapOf<String, Double>()
            val deductionsMap = mutableMapOf<String, Double>()

            val isSplit = leftColumnText != middleColumnText

            for ((key, value) in leftExtracted) {
                if (key in PayslipPatternConfig.creditKeysMapping.keys) {
                    val stdKey = PayslipPatternConfig.creditKeysMapping[key]!!
                    earningsMap[stdKey] = (earningsMap[stdKey] ?: 0.0) + value
                } else if (isSplit && key in PayslipPatternConfig.debitKeysMapping.keys) {
                    val baseStdKey = PayslipPatternConfig.debitKeysMapping[key]!!
                    val stdKey = "adj" + baseStdKey.replaceFirstChar { it.uppercaseChar() }
                    val targetKey = when (stdKey) {
                        "adjBasicPay" -> "adjBasicPay"
                        "adjDa" -> "adjDa"
                        "adjMsp" -> "adjMsp"
                        "adjTpta" -> "adjTpta"
                        "adjFieldAllowance" -> "adjFieldAllowance"
                        else -> "adjPayAndAllce"
                    }
                    earningsMap[targetKey] = (earningsMap[targetKey] ?: 0.0) + value
                }
            }

            for ((key, value) in middleExtracted) {
                if (key in PayslipPatternConfig.debitKeysMapping.keys) {
                    val stdKey = PayslipPatternConfig.debitKeysMapping[key]!!
                    deductionsMap[stdKey] = (deductionsMap[stdKey] ?: 0.0) + value
                } else if (isSplit && key in PayslipPatternConfig.creditKeysMapping.keys) {
                    val baseStdKey = PayslipPatternConfig.creditKeysMapping[key]!!
                    val stdKey = "rec" + baseStdKey.replaceFirstChar { it.uppercaseChar() }
                    val targetKey = when (stdKey) {
                        "recFieldAllowance" -> "recFieldAllowance"
                        "recSpecialForces" -> "recSpecialForces"
                        else -> "recoveryOfDebits"
                    }
                    deductionsMap[targetKey] = (deductionsMap[targetKey] ?: 0.0) + value
                }
            }

            val openingCr = earningsMap.remove("openingCreditBalance") ?: 0.0
            val closingDr = earningsMap.remove("closingDebitBalance") ?: 0.0
            val openingDr = deductionsMap.remove("openingDebitBalance") ?: 0.0
            val closingCr = deductionsMap.remove("closingCreditBalance") ?: 0.0

            val sumEarnings = earningsMap.values.sum()
            val sumDeductions = deductionsMap.values.sum()

            var realGross = grossPay
            if (openingCr > 0.0 && kotlin.math.abs(realGross - (sumEarnings + openingCr)) < 5.0) {
                realGross = sumEarnings
            }
            var realDeductions = totalDeductions
            if (realDeductions == realGross || (openingDr > 0.0 && kotlin.math.abs(realDeductions - (sumDeductions + openingDr)) < 5.0)) {
                realDeductions = sumDeductions
            }

            if (realGross == 0.0) realGross = sumEarnings
            if (realDeductions == 0.0) realDeductions = sumDeductions

            val finalNet = if (netRemittance == 0.0) {
                if (closingCr > 0.0 || closingDr > 0.0) 0.0 else realGross - realDeductions
            } else {
                netRemittance
            }

            val officer = Officer(name = officerName, accountNo = accountNo, pan = panNo)
            val earnings = Earnings(
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
                adjPayAndAllce = earningsMap["adjPayAndAllce"] ?: 0.0,
                adjFieldAllowance = earningsMap["adjFieldAllowance"] ?: 0.0,
                medicalAllowance = earningsMap["medicalAllowance"] ?: 0.0,
                adjTicketRecovery = earningsMap["adjTicketRecovery"] ?: 0.0
            )

            val deductions = Deductions(
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
                recoveryOfDebits = deductionsMap["recoveryOfDebits"] ?: 0.0
            )

            val ledgerBalances = LedgerBalances(
                openingCreditBalance = openingCr,
                openingDebitBalance = openingDr,
                closingCreditBalance = closingCr,
                closingDebitBalance = closingDr
            )

            val summary = PayslipSummary(grossPay = realGross, totalDeductions = realDeductions, netRemittance = finalNet)

            var taxAndSavings: TaxAndSavings? = null
            val taxText = if (taxPageText.isNullOrEmpty()) cleanedFullText else cleanCommasAndWhitespace(taxPageText)
            if (taxText.isNotEmpty()) {
                val grossSalMatch = Regex("Gross Salary (?:upto \\d+/\\d+/\\d+)?\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                    ?: Regex("Pay & Allce upto\\s+\\d+/\\d+/\\d+\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                val taxableIncMatch = Regex("Total Taxable Income\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                    ?: Regex("Total taxable pay\\s+\\(Sl\\.No\\.\\s*\\d+\\+\\d+\\+\\d+\\+\\d+\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                val stdDedMatch = Regex("Standard Deduction\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                val netTaxableMatch = Regex("Net Taxable Income.*?\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                    ?: Regex("Net Taxable Income\\s+\\(\\(Sl\\.No\\.\\s*\\d+\\s*\\+\\s*Sl\\.No\\.\\s*\\d+\\)\\s*-\\s*\\(Sl\\.No\\.\\s*\\d+\\)\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                val taxPayableMatch = Regex("Total Tax Payable\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                    ?: Regex("Total Income Tax\\s+\\(Tax on Sl\\.No\\.\\s*\\d+\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                val taxDeductedMatch = Regex("Income Tax Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                val cessDeductedMatch = Regex("Ed\\.\\s*Cess Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                    ?: Regex("Educ\\.\\s*Cess Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)

                if (grossSalMatch != null || taxableIncMatch != null || netTaxableMatch != null) {
                    var dsopFund: DsopFund? = null
                    val dsopText = if (dsopPageText.isNullOrEmpty()) taxText else cleanCommasAndWhitespace(dsopPageText)
                    if (dsopText.isNotEmpty()) {
                        val dsopMatch = Regex("Opening Balance\\s+(\\d+)\\s+Subscription\\s+(\\d+)\\s+Refund\\s+(\\d+)\\s+Misc Adj\\s+(\\d+)\\s+Withdrawal\\s+(\\d+)\\s+Closing Balance\\s+(\\d+)", RegexOption.IGNORE_CASE).find(dsopText)
                        if (dsopMatch != null) {
                            dsopFund = DsopFund(
                                openingBalance = dsopMatch.groupValues[1].toDoubleOrNull() ?: 0.0,
                                subscriptionYtd = dsopMatch.groupValues[2].toDoubleOrNull() ?: 0.0,
                                refundYtd = dsopMatch.groupValues[3].toDoubleOrNull() ?: 0.0,
                                miscAdjYtd = dsopMatch.groupValues[4].toDoubleOrNull() ?: 0.0,
                                withdrawalYtd = dsopMatch.groupValues[5].toDoubleOrNull() ?: 0.0,
                                closingBalance = dsopMatch.groupValues[6].toDoubleOrNull() ?: 0.0
                            )
                        }
                    }

                    taxAndSavings = TaxAndSavings(
                        grossSalaryYtd = grossSalMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                        totalTaxableIncome = taxableIncMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                        standardDeduction = stdDedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                        netTaxableIncome = netTaxableMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                        totalTaxPayable = taxPayableMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                        taxDeductedYtd = taxDeductedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                        cessDeductedYtd = cessDeductedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                        dsopFund = dsopFund
                    )
                }
            }

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
                    taxAndSavings = taxAndSavings
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
