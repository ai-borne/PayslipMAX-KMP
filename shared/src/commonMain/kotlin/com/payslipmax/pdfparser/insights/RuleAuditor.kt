package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.ServiceTimelineBuilder

interface RuleAuditor {
    fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly>
}

/**
 * An auditor that reasons over the officer's [ServiceTimeline] (level, DNI, postings) rather than raw
 * payslip pairs. The engine builds the timeline once and hands it to every such auditor; called through
 * the plain [RuleAuditor] entry point it builds its own from the history plus the current payslip.
 */
interface TimelineAuditor : RuleAuditor {
    fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        timeline: ServiceTimeline,
    ): List<Anomaly>

    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly> = audit(current, previous, ServiceTimelineBuilder.build(history + current))
}
