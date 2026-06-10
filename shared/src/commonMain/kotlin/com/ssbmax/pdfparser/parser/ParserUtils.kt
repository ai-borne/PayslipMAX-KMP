package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.*

internal fun cleanCommasAndWhitespace(text: String): String {
    val cleaned =
        text.replace(Regex("(\\d),(\\d)")) { match ->
            match.groupValues[1] + match.groupValues[2]
        }
    return cleaned.replace(Regex("\\s+"), " ")
}

/**
 * Splits the full payslip page text into a credit (earnings) section and a debit (deductions) section.
 *
 * In some older payslip formats the PDF crop box approach fails and both columns return the same
 * full text. This function finds the position of the first confirmed debit-only anchor — i.e., the
 * first occurrence of DSOPF/AGIF/ITAX/Incm Tax (items that can ONLY appear in the debit column) —
 * and splits there.
 *
 * - Text BEFORE the anchor = credit/earnings section (may contain debit labels like "L Fee" that
 *   are actually credit reversals of those deductions).
 * - Text FROM the anchor onwards = debit/deductions section.
 *
 * Returns Triple(creditSectionText, debitSectionText, anchorFound).
 * If no anchor was found, anchorFound=false and creditSection=fullText, debitSection="".
 */
internal fun splitCreditDebitSections(cleanedText: String): Triple<String, String, Boolean> {
    // These anchor labels can ONLY appear in the debit column; use the earliest one found.
    val debitOnlyAnchors = listOf("DSOPF Subn", "DSOPF", "AGIF", "Incm Tax", "ITAX")
    var splitIdx = cleanedText.length
    var found = false
    for (anchor in debitOnlyAnchors) {
        val idx = cleanedText.indexOf(anchor, ignoreCase = true)
        if (idx in 1 until splitIdx) {
            splitIdx = idx
            found = true
        }
    }
    val creditSection = cleanedText.substring(0, splitIdx)
    val debitSection = if (found) cleanedText.substring(splitIdx) else ""
    return Triple(creditSection, debitSection, found)
}

internal fun extractFromColumn(
    colText: String,
    creditMapping: Map<String, String>,
    debitMapping: Map<String, String>,
): Map<String, Double> {
    val extracted = mutableMapOf<String, Double>()
    var workingCol = cleanCommasAndWhitespace(colText)
    val keys = (creditMapping.keys + debitMapping.keys).distinct().sortedByDescending { it.length }

    for (key in keys) {
        val escapedKey = Regex.escape(key)
        val pattern =
            Regex(
                "(?<![a-zA-Z0-9])$escapedKey\\s*(?:\\([^)]+\\))?\\s*[:\\-–]?\\s*(?:Rs\\.?\\s*)?(\\d+)(?![a-zA-Z0-9])",
                RegexOption.IGNORE_CASE,
            )
        var match = pattern.find(workingCol)
        while (match != null) {
            val value = match.groupValues[1].toDoubleOrNull() ?: 0.0
            extracted[key] = (extracted[key] ?: 0.0) + value
            workingCol = workingCol.replaceFirst(match.value, "MATCHED_VALUE")
            match = pattern.find(workingCol)
        }
    }
    return extracted
}

internal fun parseDate(
    cleanedFullText: String,
    filename: String,
): Pair<Int, Int> {
    val dateMatch = Regex("STATEMENT OF ACCOUNT FOR (\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE).find(cleanedFullText)
    return if (dateMatch != null) {
        Pair(
            dateMatch.groupValues[1].toIntOrNull() ?: 1,
            dateMatch.groupValues[2].toIntOrNull() ?: 2024,
        )
    } else {
        val fileMonthMatch = Regex("\\d+\\s+([a-zA-Z]+)", RegexOption.IGNORE_CASE).find(filename)
        val fileYearMatch = Regex("(\\d{4})").find(filename)
        val mNum = fileMonthMatch?.groupValues?.get(1)?.lowercase()?.let { PayslipPatternConfig.monthMap[it] } ?: 1
        val yVal = fileYearMatch?.groupValues?.get(1)?.toIntOrNull() ?: 2024
        Pair(mNum, yVal)
    }
}

internal fun parseOfficer(cleanedFullText: String): Officer {
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
    return Officer(name = officerName, accountNo = accountNo, pan = panNo)
}

internal fun parseTotals(cleanedFullText: String): Triple<Double, Double, Double> {
    val totalsMapping =
        mapOf(
            "Gross Pay" to listOf("kula Aaya Gross Pay", "Gross Pay", "Total Credit"),
            "Total Deductions" to listOf("kula kTaOtI Total Deductions", "Total Deductions", "Total Debit"),
            "Net Remittance" to listOf("Net Remittance", "REMITTANCE", "inavala p`oiYat Qana/Net Remittance"),
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
    return Triple(
        extractedTotals["Gross Pay"] ?: 0.0,
        extractedTotals["Total Deductions"] ?: 0.0,
        extractedTotals["Net Remittance"] ?: 0.0,
    )
}

internal fun parseTaxAndSavings(
    taxPageText: String?,
    dsopPageText: String?,
    cleanedFullText: String,
): TaxAndSavings? {
    val taxText = if (taxPageText.isNullOrEmpty()) cleanedFullText else cleanCommasAndWhitespace(taxPageText)
    if (taxText.isEmpty()) return null

    val grossSalMatch =
        Regex("Gross Salary (?:upto \\d+/\\d+/\\d+)?\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Pay & Allce upto\\s+\\d+/\\d+/\\d+\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
    val taxableIncMatch =
        Regex("Total Taxable Income\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Total taxable pay\\s+\\(Sl\\.No\\.\\s*\\d+\\+\\d+\\+\\d+\\+\\d+\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
    val stdDedMatch = Regex("Standard Deduction\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
    val netTaxableMatch =
        Regex("Net Taxable Income.*?\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Net Taxable Income\\s+\\(\\(Sl\\.No\\.\\s*\\d+\\s*\\+\\s*Sl\\.No\\.\\s*\\d+\\)\\s*-\\s*\\(Sl\\.No\\.\\s*\\d+\\)\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
    val taxPayableMatch =
        Regex("Total Tax Payable\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Total Income Tax\\s+\\(Tax on Sl\\.No\\.\\s*\\d+\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
    val taxDeductedMatch = Regex("Income Tax Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
    val cessDeductedMatch =
        Regex("Ed\\.\\s*Cess Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Educ\\.\\s*Cess Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)

    if (grossSalMatch != null || taxableIncMatch != null || netTaxableMatch != null) {
        var dsopFund: DsopFund? = null
        val dsopText = if (dsopPageText.isNullOrEmpty()) taxText else cleanCommasAndWhitespace(dsopPageText)
        if (dsopText.isNotEmpty()) {
            val dsopMatch =
                Regex(
                    "Opening Balance\\s+(\\d+)\\s+Subscription\\s+(\\d+)\\s+Refund\\s+(\\d+)\\s+Misc Adj\\s+(\\d+)\\s+Withdrawal\\s+(\\d+)\\s+Closing Balance\\s+(\\d+)",
                    RegexOption.IGNORE_CASE,
                ).find(dsopText)
            if (dsopMatch != null) {
                dsopFund =
                    DsopFund(
                        openingBalance = dsopMatch.groupValues[1].toDoubleOrNull() ?: 0.0,
                        subscriptionYtd = dsopMatch.groupValues[2].toDoubleOrNull() ?: 0.0,
                        refundYtd = dsopMatch.groupValues[3].toDoubleOrNull() ?: 0.0,
                        miscAdjYtd = dsopMatch.groupValues[4].toDoubleOrNull() ?: 0.0,
                        withdrawalYtd = dsopMatch.groupValues[5].toDoubleOrNull() ?: 0.0,
                        closingBalance = dsopMatch.groupValues[6].toDoubleOrNull() ?: 0.0,
                    )
            }
        }

        return TaxAndSavings(
            grossSalaryYtd = grossSalMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
            totalTaxableIncome = taxableIncMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
            standardDeduction = stdDedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
            netTaxableIncome = netTaxableMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
            totalTaxPayable = taxPayableMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
            taxDeductedYtd = taxDeductedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
            cessDeductedYtd = cessDeductedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
            dsopFund = dsopFund,
        )
    }
    return null
}
