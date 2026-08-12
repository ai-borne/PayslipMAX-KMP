package com.payslipmax.pdfparser.insights

/**
 * Turns YTD ledger totals extracted by [TaxLedgerAggregator] into an annual income projection (D4/D5/D11,
 * Phase 3) -- extracted into its own file so the run-rate formula has a single, independently testable
 * home, mirroring [Section10CapPolicy]'s role for §10(14) capping.
 *
 * The formula treats three kinds of YTD income differently:
 * - **Regular pay** (basic/DA/MSP/allowances etc.) is annualised off the FY-calendar run rate.
 * - **Arrears** (one-off retrospective back-pay, D4) are added back verbatim -- never annualised.
 * - **Reimbursements** (refunds/recoveries of money already spent, D5) are dropped entirely -- neither
 *   annualised nor added back, since they were never taxable income.
 */
object IncomeProjectionPolicy {
    /** 12 / [monthsElapsedInFy] -- the run-rate multiplier shared by every recurring YTD total. */
    fun annualMultiplier(monthsElapsedInFy: Int): Double = 12.0 / monthsElapsedInFy

    fun projectAnnualGross(
        ytdGross: Double,
        ytdArrears: Double,
        ytdReimbursements: Double,
        monthsElapsedInFy: Int,
    ): Double {
        val regularYtd = maxOf(0.0, ytdGross - ytdArrears - ytdReimbursements)
        return (regularYtd * annualMultiplier(monthsElapsedInFy)) + ytdArrears
    }
}
