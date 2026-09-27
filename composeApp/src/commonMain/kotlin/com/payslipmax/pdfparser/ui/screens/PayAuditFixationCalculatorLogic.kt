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
 * Returns null when the latest month is untrusted (unplaced level/stage), or when [inputs.toLevel] is not
 * actually higher than the officer's current level — this is a promotion calculator, not a general
 * fixation tool, and Rule 10/11 only applies moving to a higher level.
 */
fun resolveFixationComparison(
    timeline: ServiceTimeline,
    inputs: FixationCalculatorInputs,
): PayFixationComparison? {
    val latest = timeline.months.maxByOrNull { it.month } ?: return null
    val fromLevel = latest.level ?: return null
    val fromStage = latest.stage ?: return null
    if (inputs.toLevel.ordinal <= fromLevel.ordinal) return null
    val dniMonth = timeline.events.lastOrNull { it.type == TimelineEventType.INCREMENT }?.month?.month ?: 1

    return PayFixationCalculator.compare(
        fromLevel = fromLevel,
        fromStage = fromStage,
        toLevel = inputs.toLevel,
        promotionMonth = PayMonth(inputs.promotionYear, inputs.promotionMonth),
        dniMonth = dniMonth,
    )
}
