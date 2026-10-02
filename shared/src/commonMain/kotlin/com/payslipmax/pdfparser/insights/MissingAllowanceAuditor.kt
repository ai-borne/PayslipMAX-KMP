package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth

/**
 * Flags HRA and MSP that were paid last month and are gone this month, unless the timeline explains the
 * drop: HRA stops when the officer takes government quarters (a licence fee appears); MSP is not
 * admissible to Level 14+. A missing Transport Allowance is audited by [TptaEntitlementAuditor], which
 * also covers a drop, so it is not reported twice here.
 */
class MissingAllowanceAuditor : TimelineAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        timeline: ServiceTimeline,
    ): List<Anomaly> {
        if (previous == null || current.needsReview || current.earnings.basicPay <= 0.0) return emptyList()
        val month = timeline.monthAt(PayMonth(current.year, current.monthNum))

        val checks =
            listOf(
                Triple("houseRentAllowance", "House Rent Allowance (HRA)", month?.occupiesQuarters == true),
                Triple("militaryServicePay", "Military Service Pay (MSP)", month.isLevel14OrAbove()),
            )
        return checks.mapNotNull { (field, name, explained) ->
            val prevVal = getAllowanceValue(previous, field)
            val currVal = getAllowanceValue(current, field)
            if (prevVal > 0.0 && currVal == 0.0 && !explained) {
                Anomaly(
                    type = "MISSING_ALLOWANCE",
                    field = field,
                    amount = prevVal,
                    month = current.dateStr,
                    description = "$name of ${PayAuditWording.rupees(prevVal)} was on your previous payslip but is not on this one.",
                    expected = prevVal,
                    actual = 0.0,
                )
            } else {
                null
            }
        }
    }

    private fun TimelineMonth?.isLevel14OrAbove(): Boolean = this?.level?.let { it >= PayLevel.L14 } == true

    private fun getAllowanceValue(
        payslip: ParsedPayslip,
        field: String,
    ): Double {
        return when (field) {
            "houseRentAllowance" -> payslip.earnings.houseRentAllowance
            "militaryServicePay" -> payslip.earnings.militaryServicePay
            else -> 0.0
        }
    }
}
