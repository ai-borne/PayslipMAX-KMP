package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.PayMatrix
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineEventType

/**
 * The annual increment (DNI) moves basic pay one cell down its level of the pay matrix, on 1 January or
 * 1 July. The officer's own date is learned from the timeline: an increment seen in January or July with
 * no promotion since falls due again exactly twelve months later. Flags that month if basic pay did not
 * move. Promotions reset the date, so nothing is reported until a new increment is seen; a withheld
 * increment (forfeiture) cannot be seen in a payslip, so the description says to check for one.
 */
class IncrementAuditor : TimelineAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        timeline: ServiceTimeline,
    ): List<Anomaly> {
        val now = timeline.monthAt(PayMonth(current.year, current.monthNum)) ?: return emptyList()
        val level = now.level ?: return emptyList()
        val stage = now.stage ?: return emptyList()
        val last = timeline.events.lastOrNull { it.type == TimelineEventType.INCREMENT && it.month < now.month } ?: return emptyList()
        val dueAgain = now.month.index - last.month.index == MONTHS_PER_YEAR && last.month.month in INCREMENT_MONTHS
        val promotedSince = timeline.events.any { it.type == TimelineEventType.PROMOTION && it.month > last.month && it.month <= now.month }
        if (!dueAgain || promotedSince || last.toLevel != level || now.basicPay != last.toBasicPay) return emptyList()

        val expected = PayMatrix.payAt(level, stage + 1)?.toDouble() ?: return emptyList()
        return listOf(
            Anomaly(
                type = AnomalyTierMap.INCREMENT_MISSED,
                field = "basicPay",
                amount = expected - now.basicPay,
                month = current.dateStr,
                description =
                    "Annual increment not applied: your last increment took effect in ${last.month.month}/${last.month.year}, so Basic Pay " +
                        "should move from ₹${now.basicPay.toInt()} to ₹${expected.toInt()} (Level ${level.label}). " +
                        "Check that no increment was withheld before raising it.",
                expected = expected,
                actual = now.basicPay,
                authority = PayAuthorities.ANNUAL_INCREMENT,
            ),
        )
    }

    private companion object {
        const val MONTHS_PER_YEAR = 12
        val INCREMENT_MONTHS = setOf(1, 7)
    }
}
