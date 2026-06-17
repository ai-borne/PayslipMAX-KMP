package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.*

internal fun parseTaxAndSavings(
    taxPageText: String?,
    dsopPageText: String?,
    cleanedFullText: String,
): TaxAndSavings? {
    val taxText = if (taxPageText.isNullOrEmpty()) cleanedFullText else cleanCommasAndWhitespace(taxPageText)
    if (taxText.isEmpty()) return null

    val grossSalMatch =
        Regex("Gross Salary (?:upto \\d+/\\d+/\\d+)?.*?(?<!/)\\b(\\d{4,})\\b", RegexOption.IGNORE_CASE).find(taxText)
            ?: Regex("Pay & Allce upto\\s+\\d+/\\d+/\\d+.*?(?<!/)\\b(\\d{4,})\\b", RegexOption.IGNORE_CASE).find(taxText)
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
                    "Opening Balance\\s*(\\d+)\\s*Subscription\\s*(\\d+)\\s*Refund\\s*(\\d+)\\s*Misc\\s*Adj\\s*(\\d+)\\s*Withdrawal\\s*(\\d+)\\s*Closing Balance\\s*(\\d+)",
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
