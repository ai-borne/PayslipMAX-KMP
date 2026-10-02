package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline

/**
 * Military Service Pay is a flat ₹15,500 a month for regular Army officers in Levels 10-13A. Flags a
 * payslip that pays some MSP but less than that. A month with no MSP at all is the missing-allowance
 * case ([MissingAllowanceAuditor]); more than ₹15,500 is arrears and is left alone.
 */
class MspAuditor : TimelineAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        timeline: ServiceTimeline,
    ): List<Anomaly> {
        val msp = current.earnings.militaryServicePay
        if (msp <= 0.0 || msp >= MSP_RATE || current.earnings.adjMsp != 0.0) return emptyList()
        val month = timeline.monthAt(PayMonth(current.year, current.monthNum)) ?: return emptyList()
        val level = month.level ?: return emptyList()
        if (level > PayLevel.L13A) return emptyList()

        return listOf(
            Anomaly(
                type = AnomalyTierMap.MSP_SHORTFALL,
                field = "militaryServicePay",
                amount = MSP_RATE - msp,
                month = current.dateStr,
                description =
                    "Military Service Pay (MSP) is ${PayAuditWording.rupees(msp)}, but ${PayAuditWording.rupees(MSP_RATE)} a month is due at Level ${level.label}.",
                expected = MSP_RATE,
                actual = msp,
                authority = PayAuthorities.MILITARY_SERVICE_PAY,
            ),
        )
    }

    private companion object {
        const val MSP_RATE = 15500.0
    }
}
