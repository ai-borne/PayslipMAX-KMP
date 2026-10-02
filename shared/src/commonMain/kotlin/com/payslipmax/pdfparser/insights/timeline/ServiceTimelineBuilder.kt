package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rebuilds the officer's month-by-month service history from stored payslips. Nothing is asked of the
 * user and nothing is assumed: a month is only placed when its basic pay is a 7th CPC pay-matrix cell.
 * Excluded months (precision over coverage): flagged needsReview, zero basic pay, or basic pay that is
 * not a matrix cell (pre-7th CPC pay, or a figure inflated by arrears). Duplicate months keep the first.
 */
object ServiceTimelineBuilder {
    private const val WHOLE_PERCENT_TOLERANCE = 0.1
    private const val TPTA_RATE_TOLERANCE = 0.005

    fun build(history: List<ParsedPayslip>): ServiceTimeline {
        val usable =
            history
                .filter { !it.needsReview && it.earnings.basicPay > 0.0 && PayMatrix.levelsContaining(it.earnings.basicPay).isNotEmpty() }
                .distinctBy { PayMonth(it.year, it.monthNum) }
                .sortedBy { PayMonth(it.year, it.monthNum) }

        val months = mutableListOf<TimelineMonth>()
        val events = mutableListOf<TimelineEvent>()
        var position: LevelStage? = null
        var previous: TimelineMonth? = null
        for (payslip in usable) {
            val resolved = LevelResolver.resolve(payslip.earnings.basicPay, position)
            val month = toMonth(payslip, resolved)
            previous?.let { eventBetween(it, month)?.let(events::add) }
            months += month
            previous = month
            position = resolved ?: position
        }
        return ServiceTimeline(months, events, postingSpans(usable))
    }

    private fun toMonth(
        payslip: ParsedPayslip,
        resolved: LevelStage?,
    ): TimelineMonth {
        val daPercent = wholeDaPercent(payslip)
        return TimelineMonth(
            month = PayMonth(payslip.year, payslip.monthNum),
            basicPay = payslip.earnings.basicPay,
            level = resolved?.level,
            stage = resolved?.stage,
            daPercent = daPercent,
            tptaCity = tptaCity(payslip, daPercent),
            occupiesQuarters = payslip.deductions.licenseFee > 0.0,
        )
    }

    /** DA rate actually applied: DA / (Basic + MSP), when it is a whole percent. */
    private fun wholeDaPercent(payslip: ParsedPayslip): Int? {
        val payBase = payslip.earnings.basicPay + payslip.earnings.militaryServicePay
        val rate = payslip.earnings.dearnessAllowance / payBase * 100.0
        return rate.roundToInt().takeIf { abs(rate - it) <= WHOLE_PERCENT_TOLERANCE }
    }

    /** Older payslips print TPTA inclusive of its DA, newer ones print the base and TPTA-DA separately. */
    private fun tptaCity(
        payslip: ParsedPayslip,
        daPercent: Int?,
    ): TptaCityClass? {
        val tpta = payslip.earnings.transportAllowance
        if (tpta <= 0.0) return null
        val base =
            when {
                payslip.earnings.transportAllowanceDa > 0.0 -> tpta
                daPercent != null -> tpta / (1.0 + daPercent / 100.0)
                else -> return null
            }
        return TptaCityClass.entries.firstOrNull { abs(base - it.baseRate) / it.baseRate <= TPTA_RATE_TOLERANCE }
    }

    private fun eventBetween(
        before: TimelineMonth,
        after: TimelineMonth,
    ): TimelineEvent? {
        val type =
            when {
                before.level != null && after.level != null && after.level > before.level -> TimelineEventType.PROMOTION
                before.level != null && after.level == before.level && after.stage == (before.stage ?: 0) + 1 -> TimelineEventType.INCREMENT
                else -> return null
            }
        return TimelineEvent(after.month, type, before.level, after.level, before.basicPay, after.basicPay)
    }

    private fun postingSpans(usable: List<ParsedPayslip>): List<PostingSpan> =
        listOf(
            PostingKind.RISK_HARDSHIP to { p: ParsedPayslip -> p.earnings.riskHardshipAllowance },
            PostingKind.FIELD to { p: ParsedPayslip -> p.earnings.fieldAllowance },
        ).flatMap { (kind, amount) -> spansOf(kind, usable.filter { amount(it) > 0.0 }.map { PayMonth(it.year, it.monthNum) }) }
            .sortedBy { it.first }

    private fun spansOf(
        kind: PostingKind,
        paidMonths: List<PayMonth>,
    ): List<PostingSpan> {
        val spans = mutableListOf<PostingSpan>()
        for (month in paidMonths) {
            val open = spans.lastOrNull()
            if (open != null && month.isRightAfter(open.last)) {
                spans[spans.lastIndex] = open.copy(last = month)
            } else {
                spans += PostingSpan(kind, month, month)
            }
        }
        return spans
    }
}
