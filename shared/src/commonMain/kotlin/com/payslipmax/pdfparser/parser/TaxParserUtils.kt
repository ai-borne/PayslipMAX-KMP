package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.*

internal fun parseTaxAndSavings(
    taxPageText: String?,
    dsopPageText: String?,
    cleanedFullText: String,
): TaxAndSavings? {
    val taxText = if (taxPageText.isNullOrEmpty()) cleanedFullText else cleanCommasAndWhitespace(taxPageText)
    if (taxText.isEmpty()) return null

    val grossSalMatch = findGrossSalaryMatch(taxText)
    val taxableIncMatch =
        Regex("Total Taxable Income\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Total taxable pay\\s+\\(Sl\\.No\\.\\s*\\d+\\+\\d+\\+\\d+\\+\\d+\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
    val stdDedMatch = Regex("Standard Deduction\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
    val netTaxableMatch =
        Regex("Net Taxable Income\\s+\\(\\(Sl\\.No\\.\\s*\\d+\\s*\\+\\s*Sl\\.No\\.\\s*\\d+\\)\\s*-\\s*\\(Sl\\.No\\.\\s*\\d+\\)\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Net Taxable Income\\s+\\(\\d+\\s*-\\s*\\d+\\s*-\\s*\\d+\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Net Taxable Income\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)

    if (grossSalMatch != null || taxableIncMatch != null || netTaxableMatch != null) {
        val dsopText = if (dsopPageText.isNullOrEmpty()) taxText else cleanCommasAndWhitespace(dsopPageText)
        val dsopFund = parseDsopFund(dsopText)
        val taxPayableMatch =
            Regex("Total Tax Payable\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                ?: Regex("Total Income Tax\\s+\\(Tax on Sl\\.No\\.\\s*\\d+\\)\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
        val taxDeductedMatch = Regex("Income Tax Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
        val cessDeductedMatch =
            Regex("Ed\\.\\s*Cess Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)
                ?: Regex("Educ\\.\\s*Cess Deducted\\s+(\\d+)", RegexOption.IGNORE_CASE).find(taxText)

        return buildTaxAndSavings(
            taxText = taxText,
            grossSalMatch = grossSalMatch,
            taxableIncMatch = taxableIncMatch,
            stdDedMatch = stdDedMatch,
            netTaxableMatch = netTaxableMatch,
            taxPayableMatch = taxPayableMatch,
            taxDeductedMatch = taxDeductedMatch,
            cessDeductedMatch = cessDeductedMatch,
            dsopFund = dsopFund,
        )
    }
    return null
}

private fun findGrossSalaryMatch(text: String): MatchResult? {
    val patterns =
        listOf(
            "Gross Salary (?:upto \\d+/\\d+/\\d+)?.*?\\b(\\d{4,})\\b",
            "Pay & Allce upto\\s+\\d+/\\d+/\\d+.*?\\b(\\d{4,})\\b",
        )
    for (pattern in patterns) {
        val matches = Regex(pattern, RegexOption.IGNORE_CASE).findAll(text)
        val validMatch =
            matches.firstOrNull { match ->
                val group = match.groups[1] ?: return@firstOrNull false
                val idx = group.range.first
                idx == 0 || text[idx - 1] != '/'
            }
        if (validMatch != null) return validMatch
    }
    return null
}

private fun parseDsopFund(dsopText: String): DsopFund? {
    if (dsopText.isEmpty()) return null
    val dsopMatch =
        Regex(
            "Opening Balance\\s*(\\d+)\\s*Subscription\\s*(\\d+)\\s*Refund\\s*(\\d+)\\s*Misc\\s*Adj\\s*(\\d+)\\s*Withdrawal\\s*(\\d+)\\s*Closing Balance\\s*(\\d+)",
            RegexOption.IGNORE_CASE,
        ).find(dsopText) ?: return null

    return DsopFund(
        openingBalance = dsopMatch.groupValues[1].toDoubleOrNull() ?: 0.0,
        subscriptionYtd = dsopMatch.groupValues[2].toDoubleOrNull() ?: 0.0,
        refundYtd = dsopMatch.groupValues[3].toDoubleOrNull() ?: 0.0,
        miscAdjYtd = dsopMatch.groupValues[4].toDoubleOrNull() ?: 0.0,
        withdrawalYtd = dsopMatch.groupValues[5].toDoubleOrNull() ?: 0.0,
        closingBalance = dsopMatch.groupValues[6].toDoubleOrNull() ?: 0.0,
    )
}

private fun buildTaxAndSavings(
    taxText: String,
    grossSalMatch: MatchResult?,
    taxableIncMatch: MatchResult?,
    stdDedMatch: MatchResult?,
    netTaxableMatch: MatchResult?,
    taxPayableMatch: MatchResult?,
    taxDeductedMatch: MatchResult?,
    cessDeductedMatch: MatchResult?,
    dsopFund: DsopFund?,
): TaxAndSavings {
    val grossSalaryYtd = grossSalMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    val totalTaxPayableRaw = taxPayableMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    val totalTaxPayable =
        if (grossSalaryYtd > 0.0 && totalTaxPayableRaw > grossSalaryYtd) {
            kotlin.math.round(grossSalaryYtd * 0.30)
        } else {
            totalTaxPayableRaw
        }

    val isNewRegime = taxText.contains("New Tax Regime", ignoreCase = true)
    val taxRegime = if (isNewRegime) TaxRegime.NEW else TaxRegime.OLD

    return TaxAndSavings(
        grossSalaryYtd = grossSalaryYtd,
        totalTaxableIncome = taxableIncMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
        standardDeduction = stdDedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
        netTaxableIncome = netTaxableMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
        totalTaxPayable = totalTaxPayable,
        taxDeductedYtd = taxDeductedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
        cessDeductedYtd = cessDeductedMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
        dsopFund = dsopFund,
        taxRegime = taxRegime,
    )
}
