package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TptaAbsenceExplainer
import com.payslipmax.pdfparser.insights.timeline.TptaCityClass

/**
 * Flags a month with no Transport Allowance for a Level 10-13A officer, unless the timeline explains it
 * (posting change or relocation). When [TptaAbsenceExplainer.isPendingFutureData] says a relocation can't
 * be ruled out yet because the payslips that would confirm it haven't been imported, the finding is
 * returned with [Anomaly.isPending] set instead of being suppressed (P7-17b: shown as pending rather than
 * invisible; it is never proven, so it never drafts a representation letter). The amount due is the
 * lowest rate the rule allows (other-places base plus DA at the rate the payslips applied), so it is
 * never overstated. Level 14+ has a different slab and an official-car option, so it is not audited.
 */
class TptaEntitlementAuditor : TimelineAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        timeline: ServiceTimeline,
    ): List<Anomaly> {
        val earnings = current.earnings
        if (earnings.transportAllowance > 0.0 || earnings.arrearsTpta > 0.0 || earnings.adjTpta != 0.0) return emptyList()
        val month = timeline.monthAt(PayMonth(current.year, current.monthNum)) ?: return emptyList()
        val level = month.level ?: return emptyList()
        if (month.month < FIRST_MONTH || level > PayLevel.L13A) return emptyList()
        if (TptaAbsenceExplainer.explains(timeline, month.month)) return emptyList()
        if (TptaAbsenceExplainer.isPendingFutureData(timeline, month.month)) {
            return listOf(
                Anomaly(
                    type = "TPTA_ENTITLEMENT",
                    field = "transportAllowance",
                    amount = 0.0,
                    month = current.dateStr,
                    description =
                        "Transport Allowance (TPTA) is missing from your earnings ledger for Level ${level.label}. " +
                            "This may be explained by a posting change or relocation — held pending a later payslip that would confirm it.",
                    isPending = true,
                ),
            )
        }

        val daPercent = month.daPercent ?: timeline.months.lastOrNull { it.month < month.month && it.daPercent != null }?.daPercent ?: 0
        val expected = TptaCityClass.OTHER.baseRate * (1.0 + daPercent / 100.0)
        return listOf(
            Anomaly(
                type = "TPTA_ENTITLEMENT",
                field = "transportAllowance",
                amount = expected,
                month = current.dateStr,
                description =
                    "Basic Pay is ₹${month.basicPay.toInt()} (Level ${level.label}), but Transport Allowance (TPTA) is missing " +
                        "from your earnings ledger. At least ₹${expected.toInt()} (₹${TptaCityClass.OTHER.baseRate.toInt()} + $daPercent% DA) is due.",
                expected = expected,
                actual = 0.0,
                authority = PayAuthorities.TRANSPORT_ALLOWANCE,
            ),
        )
    }

    private companion object {
        // Transport Allowance rates under the 7th CPC apply from 1 July 2017.
        val FIRST_MONTH = PayMonth(2017, 7)
    }
}
