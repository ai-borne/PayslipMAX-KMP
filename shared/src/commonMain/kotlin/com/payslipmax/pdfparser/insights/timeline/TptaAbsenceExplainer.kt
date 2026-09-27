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
