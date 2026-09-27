package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayFixationCalculator
import com.payslipmax.pdfparser.insights.timeline.PayFixationComparison
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineEventType

/** The only input a promotion (which hasn't happened yet) cannot be read off the timeline. */
data class FixationCalculatorInputs(
    val toLevel: PayLevel,
    val promotionYear: Int,
    val promotionMonth: Int,
)

/**
 * Resolves [PayFixationCalculator.compare]'s from-level/from-stage/DNI-month arguments from the
 * [ServiceTimeline] (Pay Audit Phase 6) so the calculator only ever asks the user for the one thing the
 * timeline cannot know: the promotion itself. `dniMonth` is read off the most recent INCREMENT event
 * (Army increments recur every 12 months on the same 1 Jan/1 Jul cycle, so any past one fixes the
 * officer's own cycle month); a January default is used only when no increment has been recorded yet.
 * Returns null when the latest month is untrusted (unplaced level/stage), when [inputs.toLevel] is not
 * actually higher than the officer's current level — this is a promotion calculator, not a general
 * fixation tool, and Rule 10/11 only applies moving to a higher level — or when the promotion date isn't a
 * plausible future date relative to the officer's own timeline (P7-04: unbounded year/month input, e.g.
 * 1800 or 9999, otherwise produces a technically-computed but meaningless comparison).
 */
fun resolveFixationComparison(
    timeline: ServiceTimeline,
    inputs: FixationCalculatorInputs,
): PayFixationComparison? {
    val latest = timeline.months.maxByOrNull { it.month } ?: return null
    val fromLevel = latest.level ?: return null
    val fromStage = latest.stage ?: return null
    if (inputs.toLevel.ordinal <= fromLevel.ordinal) return null
    val promotionMonth = PayMonth(inputs.promotionYear, inputs.promotionMonth)
    if (promotionMonth <= latest.month) return null
    if (promotionMonth.index - latest.month.index > MAX_PROMOTION_MONTHS_AHEAD) return null
    val dniMonth = timeline.events.lastOrNull { it.type == TimelineEventType.INCREMENT }?.month?.month ?: 1

    return PayFixationCalculator.compare(
        fromLevel = fromLevel,
        fromStage = fromStage,
        toLevel = inputs.toLevel,
        promotionMonth = promotionMonth,
        dniMonth = dniMonth,
    )
}

/** 30 years covers a full commissioned-officer career span; anything beyond is not a plausible promotion. */
private const val MAX_PROMOTION_MONTHS_AHEAD = 30 * 12
