package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.logging.Logger

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

/**
 * Outcome of reconciling the printed gross/deductions/net totals against the summed credit/debit
 * maps and the carried-over ledger balances. [miscEarnings]/[miscDeductions] absorb any residual
 * between the trusted printed totals and the parsed line items.
 */
internal data class ReconciledTotals(
    val realGross: Double,
    val realDeductions: Double,
    val finalNet: Double,
    val miscEarnings: Double,
    val miscDeductions: Double,
    val ledger: LedgerCarryOver,
    /**
     * Absolute residual between the net implied by the parsed line items and the printed net. Phase 4
     * replaced the old hard-fail (which threw and discarded the whole parse on >= 2 rupees) with this
     * signal: [ReconciliationSolver] turns it into a confidence score and a `needsReview` flag instead.
     */
    val netResidual: Double = 0.0,
)

/**
 * Resolves the trusted gross/deductions/net totals using the payslip's own arithmetic, falling back
 * to parsed sums when the printed totals are missing or self-contradictory, and computes the misc
 * residuals. Behavior extracted verbatim from [PayslipTextParser]; the hard-fail reconciliation
 * check is preserved (thrown here, caught by the parser) — Phase 4 will replace it with confidence.
 *
 * Note: this mutates the maps via [resolveLedgerBalances], which removes ledger entries so they do
 * not pollute the credit/debit sums.
 */
internal fun reconcileTotals(
    earningsMap: MutableMap<String, Double>,
    deductionsMap: MutableMap<String, Double>,
    fullText: String,
    grossPay: Double,
    totalDeductions: Double,
    netRemittance: Double,
    filename: String,
): ReconciledTotals {
    val ledger = resolveLedgerBalances(earningsMap, deductionsMap, fullText)
    val openingCr = ledger.openingCredit
    val closingDr = ledger.closingDebit
    val openingDr = ledger.openingDebit
    val closingCr = ledger.closingCredit

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

    var netResidual = 0.0
    if (netRemittance != 0.0) {
        val expectedNet =
            if (realDeductions == sumDeductions) {
                realGross - realDeductions - openingDr - closingCr
            } else {
                realGross - realDeductions
            }
        netResidual = kotlin.math.abs(expectedNet - finalNet)
        if (netResidual >= 2.0) {
            // Phase 4: no longer a hard fail. The parse is kept; the solver surfaces it via confidence.
            Logger.w("PayslipTextParser", "Net reconciliation residual ${netResidual.toInt()} for $filename (expected $expectedNet, got $finalNet)")
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

    return ReconciledTotals(
        realGross = realGross,
        realDeductions = realDeductions,
        finalNet = finalNet,
        miscEarnings = miscCr,
        miscDeductions = miscDr,
        ledger = ledger,
        netResidual = netResidual,
    )
}
