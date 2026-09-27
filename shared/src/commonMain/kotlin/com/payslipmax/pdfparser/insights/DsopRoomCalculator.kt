package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip

/** DSOP subscribed so far in the current financial year, and how much tax-free room is left under the ₹5L cap. */
data class DsopRoom(
    val financialYearLabel: String,
    val subscribedYtd: Double,
    val roomLeft: Double,
)

/**
 * DSOP subscription is tax-exempt under Sec 10(11) up to [ANNUAL_CAP] per financial year (1 April to
 * 31 March), the same cap [DsopComplianceAuditor] already cites in its 18-month-unchanged nudge (Pay
 * Audit Phase 6). Room left never goes negative — a subscription above the cap is still fully credited to
 * the DSOP fund, it simply stops being tax-exempt.
 */
object DsopRoomCalculator {
    const val ANNUAL_CAP = 500_000.0

    fun calculate(
        current: ParsedPayslip,
        history: List<ParsedPayslip>,
    ): DsopRoom {
        val fyStartYear = financialYearStart(current)
        val subscribedYtd =
            (history + current)
                .filter { financialYearStart(it) == fyStartYear }
                .distinctBy { it.year to it.monthNum }
                .sumOf { it.deductions.dsopSubscription }

        return DsopRoom(
            financialYearLabel = "FY $fyStartYear-${(fyStartYear + 1) % 100}",
            subscribedYtd = subscribedYtd,
            roomLeft = (ANNUAL_CAP - subscribedYtd).coerceAtLeast(0.0),
        )
    }

    private fun financialYearStart(payslip: ParsedPayslip): Int = if (payslip.monthNum >= 4) payslip.year else payslip.year - 1
}
