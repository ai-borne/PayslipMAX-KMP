package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlinx.serialization.Serializable

@Serializable
data class FyTaxLedgerSummary(
    val financialYear: String,
    val assessmentYear: String,
    val parsedMonthCount: Int,
    /**
     * The latest parsed payslip's calendar position within [financialYear] (Apr = 1 ... Mar = 12) --
     * the correct basis for the run-rate multiplier and for "months remaining in the FY" (D11/Phase 3).
     * Deliberately distinct from [parsedMonthCount] (how many months the user has actually uploaded,
     * which can be fewer when there are gaps) -- conflating the two understates the projection whenever
     * a month is missing before the latest upload.
     */
    val monthsElapsedInFy: Int,
    val ytdGross: Double,
    val ytdTaxDeducted: Double,
    /**
     * One-off retrospective back-pay YTD (D4), added back verbatim rather than annualised --
     * kept on the summary (not just a local in [buildFySummary]) so Phase 5's arrears-transparency
     * insight can disclose the exact figure already used to build [projectedAnnualGross].
     */
    val ytdArrears: Double,
    /** Refunds/reimbursements YTD (D5), dropped entirely from [projectedAnnualGross]. */
    val ytdReimbursements: Double,
    val ytdDsop: Double,
    val ytdAgif: Double,
    val ytdFieldAllowance: Double,
    val ytdRiskHardshipAllowance: Double,
    val ytdHra: Double,
    val ytdBasicPay: Double,
    val ytdDa: Double,
    val projectedAnnualGross: Double,
    val projectedAnnualTaxDeducted: Double,
    val projectedAnnualDsop: Double,
    val projectedAnnualAgif: Double,
    val projectedAnnualFieldAllowance: Double,
    val projectedAnnualRiskHardshipAllowance: Double,
    val projectedAnnualHra: Double,
    val projectedAnnualBasicPay: Double,
    val projectedAnnualDa: Double,
    val missingMonthNums: List<Int>,
    val latestBasicPay: Double,
    val latestDa: Double,
)

object TaxLedgerAggregator {
    fun formatIndianCurrency(amount: Double): String {
        val rounded = kotlin.math.round(amount).toLong()
        val isNegative = rounded < 0
        val absStr = kotlin.math.abs(rounded).toString()
        if (absStr.length <= 3) {
            return (if (isNegative) "-" else "") + absStr
        }
        val lastThree = absStr.takeLast(3)
        val remaining = absStr.dropLast(3)
        val formattedRemaining = remaining.reversed().chunked(2).joinToString(",").reversed()
        val result = "$formattedRemaining,$lastThree"
        return if (isNegative) "-$result" else result
    }

    /**
     * The generic Field Area allowance bucket (PCDA code "FD"), kept separate from
     * [extractRiskHardshipAllowance] so each can be capped against its own Rule 2BB category
     * (D8 -- see [Section10CapPolicy]; a merged total "cannot be category-correct").
     */
    fun extractFieldAreaAllowance(payslip: ParsedPayslip): Double {
        var total = payslip.earnings.fieldAllowance + payslip.earnings.adjFieldAllowance
        if (total <= 0.0 && payslip.rawEarnings.isNotEmpty()) {
            for ((key, value) in payslip.rawEarnings) {
                val upper = key.uppercase()
                if (upper.contains("FIELD") && riskHardshipKeywords.none { upper.contains(it) }) {
                    total += value
                }
            }
        }
        return total
    }

    /** RH11-RH33 / SICHA -- all collapse into the structured `riskHardshipAllowance` bucket at parse time. */
    fun extractRiskHardshipAllowance(payslip: ParsedPayslip): Double {
        var total = payslip.earnings.riskHardshipAllowance
        if (total <= 0.0 && payslip.rawEarnings.isNotEmpty()) {
            for ((key, value) in payslip.rawEarnings) {
                val upper = key.uppercase()
                if (riskHardshipKeywords.any { upper.contains(it) }) {
                    total += value
                }
            }
        }
        return total
    }

    private val riskHardshipKeywords = listOf("RHA", "RISK", "HARDSHIP", "SICHA")

    /**
     * D4 fix: PCDA codes retrospective back-pay as `ARR-*` (e.g. `ARR-DA`, `ARR-RH11`), which
     * [PayslipPatternConfig.creditKeysMapping] resolves into **structured** `arrears*` fields on
     * [com.payslipmax.pdfparser.domain.Earnings] at parse time -- they never reach [ParsedPayslip.rawEarnings].
     * The old implementation matched an English keyword list ("ARREAR"/"BACKPAY") against `rawEarnings`
     * only, so it always summed to zero for every real payslip. Read the structured fields directly (SSOT:
     * they *are* the parsed model, not a second keyword list), then fall back to the `ARR-` prefix -- the
     * one convention PCDA's own codes already use -- for any code not yet in the mapping.
     */
    fun extractNonRecurringArrears(payslip: ParsedPayslip): Double {
        val e = payslip.earnings
        var sum =
            e.arrearsCea + e.arrearsDa + e.arrearsRation + e.arrearsSpecialForces +
                e.arrearsTpta + e.arrearsTptaDa + e.arrearsHra + e.arrearsRiskHardship
        for ((key, value) in payslip.rawEarnings) {
            if (key.uppercase().startsWith("ARR-")) {
                sum += value
            }
        }
        return sum
    }

    /**
     * D5 fix: [PayslipPatternConfig.creditKeysMapping] maps refund/reimbursement-style one-off codes
     * ("ETKT-ref" ticket reimbursement, "Ref.L Fee"/"Ref.Furn." deduction refunds, "LTC Encash",
     * "Adhoc Payt") into `adjTicketRecovery`/`adjPayAndAllce`. Unlike arrears, these are not taxable
     * back-pay to annualise or add back -- they are excluded from the taxable projection entirely (the
     * ETKT credit already nets against its own `ticketRecovery` deduction; the others are non-recurring
     * refunds of money the officer already paid). Falls back to the raw `ETKT` code for unmapped credits.
     */
    fun extractReimbursements(payslip: ParsedPayslip): Double {
        var sum = payslip.earnings.adjTicketRecovery + payslip.earnings.adjPayAndAllce
        for ((key, value) in payslip.rawEarnings) {
            if (key.uppercase().startsWith("ETKT")) {
                sum += value
            }
        }
        return sum
    }

    /** Apr = 1 ... Mar = 12 -- the FY-relative calendar position used for the run-rate multiplier (D11). */
    fun monthPositionInFy(monthNum: Int): Int = if (monthNum >= 4) monthNum - 3 else monthNum + 9

    fun computeFinancialYear(
        year: Int,
        monthNum: Int,
    ): String {
        val startYear = if (monthNum >= 4) year else year - 1
        val endYearShort = (startYear + 1) % 100
        val endYearStr = if (endYearShort < 10) "0$endYearShort" else "$endYearShort"
        return "$startYear-$endYearStr"
    }

    fun computeAssessmentYear(financialYear: String): String {
        val parts = financialYear.split("-")
        if (parts.size != 2) return financialYear
        val startYear = parts[0].toIntOrNull() ?: return financialYear
        val ayStart = startYear + 1
        val ayEndShort = (ayStart + 1) % 100
        val ayEndStr = if (ayEndShort < 10) "0$ayEndShort" else "$ayEndShort"
        return "$ayStart-$ayEndStr"
    }

    fun aggregateFy(
        payslips: List<ParsedPayslip>,
        targetFy: String? = null,
    ): FyTaxLedgerSummary {
        if (payslips.isEmpty()) {
            return createEmptySummary(targetFy ?: "2024-25")
        }

        val activeFy = targetFy ?: computeFinancialYear(payslips.last().year, payslips.last().monthNum)
        val fyPayslips =
            payslips.filter { computeFinancialYear(it.year, it.monthNum) == activeFy }
                .sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum })

        if (fyPayslips.isEmpty()) {
            return createEmptySummary(activeFy)
        }

        return buildFySummary(activeFy, fyPayslips)
    }

    private fun buildFySummary(
        activeFy: String,
        fyPayslips: List<ParsedPayslip>,
    ): FyTaxLedgerSummary {
        val count = fyPayslips.size
        val latest = fyPayslips.last()

        // D11: the multiplier must reflect how far into the FY the latest payslip actually is, not how
        // many months were uploaded -- a user who skipped a month but has a July payslip is 4 months into
        // the FY, not 3, and annualising off the smaller count understates the projection.
        val monthsElapsed = monthPositionInFy(latest.monthNum)
        val multiplier = IncomeProjectionPolicy.annualMultiplier(monthsElapsed)

        val ytdGross = fyPayslips.sumOf { it.summary.grossPay }
        val ytdArrears = fyPayslips.sumOf { extractNonRecurringArrears(it) }
        val ytdReimbursements = fyPayslips.sumOf { extractReimbursements(it) }
        val projectedGross = IncomeProjectionPolicy.projectAnnualGross(ytdGross, ytdArrears, ytdReimbursements, monthsElapsed)

        // D11: PCDA's own YTD counters on the latest payslip are the ground truth for tax-paid-so-far
        // (and, unlike the old code, include cess) -- summing each parsed payslip's single-month `ITAX`
        // line both misses cess and undercounts whenever a month wasn't uploaded. Fall back to the summed
        // ledger only when PCDA's own figure is unavailable (e.g. a synthetic/test payslip).
        val pcdaYtdTax = (latest.taxAndSavings?.taxDeductedYtd ?: 0.0) + (latest.taxAndSavings?.cessDeductedYtd ?: 0.0)
        val ledgerYtdTax = fyPayslips.sumOf { it.deductions.incomeTax + it.deductions.educationCess }
        val ytdTax = if (pcdaYtdTax > 0.0) pcdaYtdTax else ledgerYtdTax

        val ytdDsop = fyPayslips.sumOf { it.deductions.dsopSubscription }
        val ytdAgif = fyPayslips.sumOf { it.deductions.agif }
        val ytdField = fyPayslips.sumOf { extractFieldAreaAllowance(it) }
        val ytdRiskHardship = fyPayslips.sumOf { extractRiskHardshipAllowance(it) }
        val ytdHra = fyPayslips.sumOf { it.earnings.houseRentAllowance }
        val ytdBasic = fyPayslips.sumOf { it.earnings.basicPay }
        val ytdDa = fyPayslips.sumOf { it.earnings.dearnessAllowance }

        val presentMonths = fyPayslips.map { it.monthNum }.toSet()
        val missingMonths = (1..12).filter { m -> !presentMonths.contains(m) }

        return FyTaxLedgerSummary(
            financialYear = activeFy,
            assessmentYear = computeAssessmentYear(activeFy),
            parsedMonthCount = count,
            monthsElapsedInFy = monthsElapsed,
            ytdGross = ytdGross,
            ytdTaxDeducted = ytdTax,
            ytdArrears = ytdArrears,
            ytdReimbursements = ytdReimbursements,
            ytdDsop = ytdDsop,
            ytdAgif = ytdAgif,
            ytdFieldAllowance = ytdField,
            ytdRiskHardshipAllowance = ytdRiskHardship,
            ytdHra = ytdHra,
            ytdBasicPay = ytdBasic,
            ytdDa = ytdDa,
            projectedAnnualGross = projectedGross,
            projectedAnnualTaxDeducted = ytdTax * multiplier,
            projectedAnnualDsop = ytdDsop * multiplier,
            projectedAnnualAgif = ytdAgif * multiplier,
            projectedAnnualFieldAllowance = ytdField * multiplier,
            projectedAnnualRiskHardshipAllowance = ytdRiskHardship * multiplier,
            projectedAnnualHra = ytdHra * multiplier,
            projectedAnnualBasicPay = ytdBasic * multiplier,
            projectedAnnualDa = ytdDa * multiplier,
            missingMonthNums = missingMonths,
            latestBasicPay = latest.earnings.basicPay,
            latestDa = latest.earnings.dearnessAllowance,
        )
    }

    private fun createEmptySummary(fy: String): FyTaxLedgerSummary {
        return FyTaxLedgerSummary(
            financialYear = fy,
            assessmentYear = computeAssessmentYear(fy),
            parsedMonthCount = 0,
            monthsElapsedInFy = 1,
            ytdGross = 0.0,
            ytdTaxDeducted = 0.0,
            ytdArrears = 0.0,
            ytdReimbursements = 0.0,
            ytdDsop = 0.0,
            ytdAgif = 0.0,
            ytdFieldAllowance = 0.0,
            ytdRiskHardshipAllowance = 0.0,
            ytdHra = 0.0,
            ytdBasicPay = 0.0,
            ytdDa = 0.0,
            projectedAnnualGross = 0.0,
            projectedAnnualTaxDeducted = 0.0,
            projectedAnnualDsop = 0.0,
            projectedAnnualAgif = 0.0,
            projectedAnnualFieldAllowance = 0.0,
            projectedAnnualRiskHardshipAllowance = 0.0,
            projectedAnnualHra = 0.0,
            projectedAnnualBasicPay = 0.0,
            projectedAnnualDa = 0.0,
            missingMonthNums = (1..12).toList(),
            latestBasicPay = 0.0,
            latestDa = 0.0,
        )
    }
}
