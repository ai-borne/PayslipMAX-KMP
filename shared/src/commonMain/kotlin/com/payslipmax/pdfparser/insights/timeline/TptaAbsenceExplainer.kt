package com.payslipmax.pdfparser.insights.timeline

import kotlin.math.abs

/**
 * Decides whether a month with no Transport Allowance is explained by the officer's own history, so it
 * is not reported as an entitlement the payslip failed to pay. Two structural explanations, both read
 * from the timeline: the month sits at the edge of a posting change (a Risk & Hardship / field span
 * starts or ends), or the TPTA city class differs before and after the gap (the officer relocated).
 */
internal object TptaAbsenceExplainer {
    private const val RELOCATION_WINDOW_MONTHS = 6

    fun explains(
        timeline: ServiceTimeline,
        month: PayMonth,
    ): Boolean = atPostingChange(timeline, month) || relocated(timeline, month)

    /**
     * True when [explains] says no today only because the payslips that would confirm or rule out a
     * relocation ([relocated] needs a same-window sample on both sides) have not been imported yet — not
     * because none exists. A finding held for this reason should stay unflagged rather than fire and never
     * retract (P7-17, docs/Plan/09_PayAudit_PhasePlan.md): once a later payslip is imported and the same
     * month is re-audited, [explains] settles the question for good, one way or the other.
     */
    fun isPendingFutureData(
        timeline: ServiceTimeline,
        month: PayMonth,
    ): Boolean {
        if (explains(timeline, month)) return false
        val hasPriorCitySample =
            timeline.months.any { it.tptaCity != null && it.month < month && month.index - it.month.index <= RELOCATION_WINDOW_MONTHS }
        if (!hasPriorCitySample) return false
        val hasFutureDataInWindow = timeline.months.any { it.month > month && it.month.index - month.index <= RELOCATION_WINDOW_MONTHS }
        return !hasFutureDataInWindow
    }

    private fun atPostingChange(
        timeline: ServiceTimeline,
        month: PayMonth,
    ): Boolean =
        timeline.postings.any {
            month.index in (it.first.index - 1)..it.first.index || month.index in it.last.index..(it.last.index + 1)
        }

    private fun relocated(
        timeline: ServiceTimeline,
        month: PayMonth,
    ): Boolean {
        val withCity = timeline.months.filter { it.tptaCity != null && abs(it.month.index - month.index) <= RELOCATION_WINDOW_MONTHS }
        val before = withCity.lastOrNull { it.month < month }
        val after = withCity.firstOrNull { it.month > month }
        return before != null && after != null && before.tptaCity != after.tptaCity
    }
}
