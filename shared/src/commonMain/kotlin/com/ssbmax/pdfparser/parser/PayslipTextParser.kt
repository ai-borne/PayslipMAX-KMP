package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.*

object PayslipTextParser {

    private val monthMap = mapOf(
        "january" to 1, "jan" to 1,
        "february" to 2, "feb" to 2,
        "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4,
        "may" to 5,
        "june" to 6, "jun" to 6,
        "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8,
        "september" to 9, "sep" to 9,
        "october" to 10, "oct" to 10,
        "november" to 11, "nov" to 11,
        "december" to 12, "dec" to 12
    )

    private val creditKeysMapping = mapOf(
        "Basic Pay" to "basicPay",
        "BPAY" to "basicPay",
        "DA" to "dearnessAllowance",
        "MSP" to "militaryServicePay",
        "Tpt Allc" to "transportAllowance",
        "TPTA" to "transportAllowance",
        "TPTADA" to "transportAllowanceDa",
        "Tpt DA" to "transportAllowanceDa",
        "DRESALW" to "dressAllowance",
        "A/o DressAllowance" to "dressAllowance",
        "RSHNA" to "rationMoney",
        "RMONEYAllce-RA" to "rationMoney",
        "RA" to "rationMoney",
        "SpCmd Pay" to "specialForcesPay",
        "SPCDO" to "specialForcesPay",
        "SC" to "specialForcesPay",
        "FD" to "fieldAllowance",
        "CEA" to "childrenEducationAllowance",
        "Op Cr Bal" to "openingCreditBalance",
        "Cl. Dr. Bal." to "closingDebitBalance",
        "ARR-CEA" to "arrearsCea",
        "ARR-DA" to "arrearsDa",
        "ARR-RSHNA" to "arrearsRation",
        "ARR-SPCDO" to "arrearsSpecialForces",
        "ARR-TPTA" to "arrearsTpta",
        "ARR-TPTADA" to "arrearsTptaDa",
        "A/o BPAY-" to "adjBasicPay",
        "A/o DA-" to "adjDa",
        "A/o MSP-" to "adjMsp",
        "A/o TRAN-2" to "adjTpta"
    )

    private val debitKeysMapping = mapOf(
        "DSOPF Subn" to "dsopSubscription",
        "DSOP" to "dsopSubscription",
        "AGIF" to "agif",
        "Incm Tax" to "incomeTax",
        "ITAX" to "incomeTax",
        "Educ Cess" to "educationCess",
        "EHCESS" to "educationCess",
        "L Fee" to "licenseFee",
        "LF" to "licenseFee",
        "Fur" to "furnitureRent",
        "FUR" to "furnitureRent",
        "Water" to "waterCharges",
        "WATER" to "waterCharges",
        "Elec" to "electricityCharges",
        "Barrack Damage" to "barrackDamage",
        "Dr Barrack Damage" to "barrackDamage",
        "ETKT" to "ticketRecovery",
        "R/o Etkt" to "ticketRecovery",
        "Rec CIA-FD" to "recFieldAllowance",
        "Rec PARA-SC" to "recSpecialForces",
        "Op Dr Bal" to "openingDebitBalance",
        "Cl. Cr. Bal." to "closingCreditBalance",
        "R/o Of /Drs" to "recoveryOfDebits"
    )

    private val monthNames = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    fun parse(flatText: String, filename: String): Result<ParsedPayslip> {
        return try {
            // Clean commas in numbers (e.g. 1,39,604 -> 139604)
            var cleanedText = flatText.replace(Regex("(\\d),(\\d)")) { match ->
                match.groupValues[1] + match.groupValues[2]
            }
            cleanedText = cleanedText.replace(Regex("\\s+"), " ")

            // Extract month and year from text, or fallback to filename
            val dateMatch = Regex("STATEMENT OF ACCOUNT FOR (\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE).find(cleanedText)
            val (monthNum, year) = if (dateMatch != null) {
                Pair(
                    dateMatch.groupValues[1].toIntOrNull() ?: 1,
                    dateMatch.groupValues[2].toIntOrNull() ?: 2024
                )
            } else {
                // Fallback: parse from filename e.g. "01 January 2022.pdf" or "01 Jan 2023.pdf"
                val fileMonthMatch = Regex("\\d+\\s+([a-zA-Z]+)", RegexOption.IGNORE_CASE).find(filename)
                val fileYearMatch = Regex("(\\d{4})").find(filename)
                val mNum = fileMonthMatch?.groupValues?.get(1)?.lowercase()?.let { monthMap[it] } ?: 1
                val yVal = fileYearMatch?.groupValues?.get(1)?.toIntOrNull() ?: 2024
                Pair(mNum, yVal)
            }
            val monthName = monthNames.getOrNull(monthNum) ?: "January"

            // Extract Name, CDA A/C No, PAN
            val nameRegex = Regex("Name\\s*:\\s*([A-Za-z\\s]+)", RegexOption.IGNORE_CASE)
            val acRegex = Regex("(?:A/C No|CDA A/C NO)\\s*[:\\-–]?\\s*([^\\s]+)", RegexOption.IGNORE_CASE)
            val panRegex = Regex("PAN No\\s*:\\s*([^\\s]+)", RegexOption.IGNORE_CASE)

            var officerName = nameRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: "Officer Officer Officer"
            officerName = officerName.split(Regex("A/C|Email|PAN|Basic|BPAY|CDA", RegexOption.IGNORE_CASE))[0].trim()
            if (officerName.endsWith(" A", ignoreCase = true)) {
                officerName = officerName.substring(0, officerName.length - 2).trim()
            }

            var accountNo = acRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: "16/000/000000X"
            if (accountNo.endsWith("PAN")) {
                accountNo = accountNo.removeSuffix("PAN").trim()
            }
            if (accountNo.startsWith(":")) {
                accountNo = accountNo.removePrefix(":").trim()
            }

            val panNo = panRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: "AR*****90G"

            // List of all keys to match, sorted by length descending
            val searchKeys = (creditKeysMapping.keys + debitKeysMapping.keys +
                    listOf("Total Credit", "Total Debit", "Gross Pay", "Total Deductions", "Net Remittance", "REMITTANCE"))
                .distinct()
                .sortedByDescending { it.length }

            val extractedValues = mutableMapOf<String, Double>()
            var workingText = cleanedText
            
            for (key in searchKeys) {
                val escapedKey = Regex.escape(key)
                val pattern = Regex("$escapedKey\\s*[:\\-–]?\\s*(?:Rs\\.?\\s*)?(\\d+)", RegexOption.IGNORE_CASE)
                val match = pattern.find(workingText)
                if (match != null) {
                    val value = match.groupValues[1].toDoubleOrNull() ?: 0.0
                    extractedValues[key] = value
                    workingText = workingText.replaceFirst(pattern, "MATCHED_VALUE")
                }
            }

            // Map totals
            val grossPay = extractedValues["Gross Pay"] ?: extractedValues["Total Credit"] ?: 0.0
            val totalDeductions = extractedValues["Total Deductions"] ?: extractedValues["Total Debit"] ?: 0.0
            val netRemittance = extractedValues["Net Remittance"] ?: extractedValues["REMITTANCE"] ?: 0.0

            // Extract Earnings & Deductions
            val earningsMap = mutableMapOf<String, Double>()
            val deductionsMap = mutableMapOf<String, Double>()
            val ledgerBalancesMap = mutableMapOf<String, Double>()

            for ((rawKey, stdKey) in creditKeysMapping) {
                val value = extractedValues[rawKey] ?: 0.0
                if (value > 0.0) {
                    if (stdKey.startsWith("opening") || stdKey.startsWith("closing")) {
                        ledgerBalancesMap[stdKey] = value
                    } else {
                        earningsMap[stdKey] = value
                    }
                }
            }

            for ((rawKey, stdKey) in debitKeysMapping) {
                val value = extractedValues[rawKey] ?: 0.0
                if (value > 0.0) {
                    if (stdKey.startsWith("opening") || stdKey.startsWith("closing")) {
                        ledgerBalancesMap[stdKey] = value
                    } else {
                        deductionsMap[stdKey] = value
                    }
                }
            }

            // Reconstruct final objects
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
                arrearsTptaDa = earningsMap["arrearsTptaDa"] ?: 0.0
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
                openingCreditBalance = ledgerBalancesMap["openingCreditBalance"] ?: 0.0,
                openingDebitBalance = ledgerBalancesMap["openingDebitBalance"] ?: 0.0,
                closingCreditBalance = ledgerBalancesMap["closingCreditBalance"] ?: 0.0,
                closingDebitBalance = ledgerBalancesMap["closingDebitBalance"] ?: 0.0
            )

            // Final Gross, Deductions, and Net adjustments
            val finalGross = if (grossPay == 0.0) earningsMap.values.sum() else grossPay
            val finalDeductions = if (totalDeductions == 0.0 || totalDeductions == finalGross) deductionsMap.values.sum() else totalDeductions
            val finalNet = if (netRemittance == 0.0) {
                // If there's a closing ledger balance or no remittance key, it means net remittance is 0
                if (ledgerBalances.closingCreditBalance > 0.0 || ledgerBalances.closingDebitBalance > 0.0) {
                    0.0
                } else {
                    finalGross - finalDeductions
                }
            } else {
                netRemittance
            }

            val summary = PayslipSummary(grossPay = finalGross, totalDeductions = finalDeductions, netRemittance = finalNet)

            // Parse tax and savings details from Page 3 if present
            var taxAndSavings: TaxAndSavings? = null
            
            val grossSalMatch = Regex("Gross Salary (?:upto \\d+/\\d+/\\d+)?\\s+(\\d+)", RegexOption.IGNORE_CASE).find(cleanedText)
            val taxableIncMatch = Regex("Total Taxable Income\\s+(\\d+)", RegexOption.IGNORE_CASE).find(cleanedText)
            val stdDedMatch = Regex("Standard Deduction\\s+(\\d+)", RegexOption.IGNORE_CASE).find(cleanedText)
            val netTaxableMatch = Regex("Net Taxable Income.*?\\s+(\\d+)", RegexOption.IGNORE_CASE).find(cleanedText)
            val taxPayableMatch = Regex("Total Tax Payable\\s+(\\d+)", RegexOption.IGNORE_CASE).find(cleanedText)
            val taxDeductedMatch = Regex("Income Tax Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(cleanedText)
            val cessDeductedMatch = Regex("Ed\\.\\s*Cess Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(cleanedText)

            if (grossSalMatch != null || taxableIncMatch != null) {
                // Parse DSOP Fund Details
                val dsopMatch = Regex("Opening Balance\\s+(\\d+)\\s+Subscription\\s+(\\d+)\\s+Refund\\s+(\\d+)\\s+Misc Adj\\s+(\\d+)\\s+Withdrawal\\s+(\\d+)\\s+Closing Balance\\s+(\\d+)", RegexOption.IGNORE_CASE).find(cleanedText)
                val dsopFund = if (dsopMatch != null) {
                    DsopFund(
                        openingBalance = dsopMatch.groupValues[1].toDoubleOrNull() ?: 0.0,
                        subscriptionYtd = dsopMatch.groupValues[2].toDoubleOrNull() ?: 0.0,
                        refundYtd = dsopMatch.groupValues[3].toDoubleOrNull() ?: 0.0,
                        miscAdjYtd = dsopMatch.groupValues[4].toDoubleOrNull() ?: 0.0,
                        withdrawalYtd = dsopMatch.groupValues[5].toDoubleOrNull() ?: 0.0,
                        closingBalance = dsopMatch.groupValues[6].toDoubleOrNull() ?: 0.0
                    )
                } else null

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
