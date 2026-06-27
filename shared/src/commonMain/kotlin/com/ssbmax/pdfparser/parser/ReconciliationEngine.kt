package com.ssbmax.pdfparser.parser

/** Carried-over ledger balances reconciled from the parsed maps and the raw payslip text. */
internal data class LedgerCarryOver(
    val openingCredit: Double,
    val openingDebit: Double,
    val closingCredit: Double,
    val closingDebit: Double,
)

/**
 * Resolves the four ledger carry-over balances. Values are taken from the already-classified
 * earnings/deductions maps (removing them so they don't pollute the credit/debit totals) and, when
 * absent there, recovered via fallback regexes over the raw full text. Behavior extracted verbatim
 * from [PayslipTextParser] to keep that file within the 300-line limit.
 */
internal fun resolveLedgerBalances(
    earningsMap: MutableMap<String, Double>,
    deductionsMap: MutableMap<String, Double>,
    fullText: String,
): LedgerCarryOver {
    var openingCr = earningsMap.remove("openingCreditBalance") ?: 0.0
    var closingDr = earningsMap.remove("closingDebitBalance") ?: 0.0
    var openingDr = deductionsMap.remove("openingDebitBalance") ?: 0.0
    var closingCr = deductionsMap.remove("closingCreditBalance") ?: 0.0

    if (openingCr == 0.0) {
        val m =
            Regex("""Op\s*Cr\s*Bal[\.\s]*(\d+)""", RegexOption.IGNORE_CASE).find(fullText)
                ?: Regex("""OP\s*Bal\(\+\)[\.\s]*(\d+)""", RegexOption.IGNORE_CASE).find(fullText)
        if (m != null) openingCr = m.groupValues[1].toDoubleOrNull() ?: 0.0
    }
    if (closingDr == 0.0) {
        val m =
            Regex("""Cl\.\s*Dr\.\s*Bal[\.\s]*(\d+)""", RegexOption.IGNORE_CASE).find(fullText)
                ?: Regex("""Clos\s*Bal\(-\)[\.\s]*(\d+)""", RegexOption.IGNORE_CASE).find(fullText)
        if (m != null) closingDr = m.groupValues[1].toDoubleOrNull() ?: 0.0
    }
    if (openingDr == 0.0) {
        val m =
            Regex("""Op\s*Dr\s*Bal[\.\s]*(\d+)""", RegexOption.IGNORE_CASE).find(fullText)
                ?: Regex("""OP\s*Bal\(-\)[\.\s]*(\d+)""", RegexOption.IGNORE_CASE).find(fullText)
        if (m != null) openingDr = m.groupValues[1].toDoubleOrNull() ?: 0.0
    }
    if (closingCr == 0.0) {
        val m =
            Regex("""Cl\.\s*Cr\.\s*Bal[\.\s]*(\d+)""", RegexOption.IGNORE_CASE).find(fullText)
                ?: Regex("""Clos\s*Bal\(\+\)[\.\s]*(\d+)""", RegexOption.IGNORE_CASE).find(fullText)
        if (m != null) closingCr = m.groupValues[1].toDoubleOrNull() ?: 0.0
    }

    return LedgerCarryOver(
        openingCredit = openingCr,
        openingDebit = openingDr,
        closingCredit = closingCr,
        closingDebit = closingDr,
    )
}
