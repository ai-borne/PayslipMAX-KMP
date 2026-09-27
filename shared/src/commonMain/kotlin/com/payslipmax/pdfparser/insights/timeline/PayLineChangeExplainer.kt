package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.math.abs

/**
 * Explains a month-to-month move in the pay-line fields the [ServiceTimeline] already models (Phases 1-2
 * of docs/Plan/09_PayAudit_PhasePlan.md): Basic Pay (DNI/promotion), DA and its arrears (rate revision),
 * Transport Allowance (posting change, relocation, or DA), HRA/licence fee (quarters), and Risk &
 * Hardship/Field allowance (posting spans). Fields the timeline does not model (income tax, DSOP, one-off
 * adjustments, non-DA arrears) are out of scope — a changed field with no matching rule below comes back
 * with [ChangeExplanation.reason] null rather than a guess, so coverage can be measured honestly.
 */
object PayLineChangeExplainer {
    private const val AMOUNT_TOLERANCE = 0.5

    /**
     * Explains [current] against the last trustworthy month before it in [timeline] — never the
     * immediately-preceding stored payslip directly, since that payslip's own month may itself be
     * excluded from the timeline (needsReview, zero/non-matrix basic pay). Walking back to the last
     * trusted month (rather than skipping the transition outright) means a gap in the stored history
     * no longer silently drops the explanation for the next trustworthy month that follows it.
     */
    fun explain(
        current: ParsedPayslip,
        history: List<ParsedPayslip>,
        timeline: ServiceTimeline,
    ): List<ChangeExplanation> {
        if (current.needsReview) return emptyList()
        val month = PayMonth(current.year, current.monthNum)
        val currMonth = timeline.monthAt(month) ?: return emptyList()
        val prevMonth = timeline.months.lastOrNull { it.month < currMonth.month } ?: return emptyList()
        val previous =
            history.firstOrNull { it.year == prevMonth.month.year && it.monthNum == prevMonth.month.month } ?: return emptyList()

        return trackedFields(previous, current)
            .filter { (_, from, to) -> abs(from - to) > AMOUNT_TOLERANCE }
            .map { (field, from, to) ->
                ChangeExplanation(month, field, from, to, reasonFor(field, from, to, prevMonth, currMonth, current, timeline))
            }
    }

    /**
     * [explain] for every stored month against the one before it (Phase 8 P7-12) — covers the whole
     * [ServiceTimeline], not just [current]'s own transition.
     */
    fun explainAll(
        history: List<ParsedPayslip>,
        timeline: ServiceTimeline,
    ): List<ChangeExplanation> = history.flatMap { explain(it, history, timeline) }

    private fun trackedFields(
        previous: ParsedPayslip,
        current: ParsedPayslip,
    ): List<Triple<String, Double, Double>> =
        listOf(
            Triple("basicPay", previous.earnings.basicPay, current.earnings.basicPay),
            Triple("dearnessAllowance", previous.earnings.dearnessAllowance, current.earnings.dearnessAllowance),
            Triple("arrearsDa", previous.earnings.arrearsDa, current.earnings.arrearsDa),
            Triple("arrearsTptaDa", previous.earnings.arrearsTptaDa, current.earnings.arrearsTptaDa),
            Triple("militaryServicePay", previous.earnings.militaryServicePay, current.earnings.militaryServicePay),
            Triple("transportAllowance", tptaTotal(previous), tptaTotal(current)),
            Triple("houseRentAllowance", previous.earnings.houseRentAllowance, current.earnings.houseRentAllowance),
            Triple("licenseFee", previous.deductions.licenseFee, current.deductions.licenseFee),
            Triple("riskHardshipAllowance", previous.earnings.riskHardshipAllowance, current.earnings.riskHardshipAllowance),
            Triple("fieldAllowance", previous.earnings.fieldAllowance, current.earnings.fieldAllowance),
        )

    private fun tptaTotal(payslip: ParsedPayslip): Double = payslip.earnings.transportAllowance + payslip.earnings.transportAllowanceDa

    private fun reasonFor(
        field: String,
        from: Double,
        to: Double,
        prevMonth: TimelineMonth,
        currMonth: TimelineMonth,
        current: ParsedPayslip,
        timeline: ServiceTimeline,
    ): String? =
        when (field) {
            "basicPay" -> basicPayReason(currMonth, timeline)
            "dearnessAllowance" -> daReason(prevMonth, currMonth, timeline)
            "arrearsDa" -> arrearsReason(from, to, prevMonth, currMonth, current, tptaDaSeparate = false)
            "arrearsTptaDa" -> arrearsReason(from, to, prevMonth, currMonth, current, tptaDaSeparate = true)
            "militaryServicePay" -> mspReason(currMonth, current)
            "transportAllowance" -> tptaReason(prevMonth, currMonth, timeline)
            "houseRentAllowance", "licenseFee" -> quartersReason(prevMonth, currMonth)
            "riskHardshipAllowance" -> postingReason(PostingKind.RISK_HARDSHIP, currMonth.month, timeline, "Risk & Hardship")
            "fieldAllowance" -> postingReason(PostingKind.FIELD, currMonth.month, timeline, "Field")
            else -> null
        }

    private fun basicPayReason(
        currMonth: TimelineMonth,
        timeline: ServiceTimeline,
    ): String? =
        when (timeline.events.firstOrNull { it.month == currMonth.month }?.type) {
            TimelineEventType.PROMOTION -> "Promotion to Level ${currMonth.level?.label}"
            TimelineEventType.INCREMENT -> "Annual increment (Level ${currMonth.level?.label}, stage ${currMonth.stage})"
            null -> null
        }

    /** DA moves either because the rate changed, or because it is a percentage of a pay base that itself just rose (DNI/promotion). */
    private fun daReason(
        prevMonth: TimelineMonth,
        currMonth: TimelineMonth,
        timeline: ServiceTimeline,
    ): String? {
        val prevDa = prevMonth.daPercent
        val currDa = currMonth.daPercent
        if (prevDa != null && currDa != null && prevDa != currDa) return "DA revised $prevDa%→$currDa%"
        val payBaseEvent = timeline.events.firstOrNull { it.month == currMonth.month } ?: return null
        return "DA follows the ${payBaseEvent.type.name.lowercase()} pay-base rise"
    }

    /**
     * DA (or separately-printed TPTA-DA) arrears are paid the month a rise first appears, for the months
     * since its effective date, and never repeat the month after — a one-off back-payment, not a new rate.
     */
    private fun arrearsReason(
        from: Double,
        to: Double,
        prevMonth: TimelineMonth,
        currMonth: TimelineMonth,
        current: ParsedPayslip,
        tptaDaSeparate: Boolean,
    ): String? {
        if (to == 0.0 && from > 0.0) return "One-off arrears payment, not recurring"
        val prevDa = prevMonth.daPercent
        val currDa = currMonth.daPercent
        if (prevDa == null || currDa == null || currDa <= prevDa) return null
        if (tptaDaSeparate && current.earnings.transportAllowanceDa <= 0.0) return null
        val (rangeFrom, rangeTo) = arrearsRange(current.monthNum) ?: return null
        val label = if (tptaDaSeparate) "TPTA DA" else "DA"
        return "$label revised $prevDa%→$currDa%, arrears for $rangeFrom/${current.year}–$rangeTo/${current.year}"
    }

    /** Months from the rise's effective date (1 Jan or 1 Jul) up to the month before this payslip; mirrors DaArrearsAuditor. */
    private fun arrearsRange(monthNum: Int): Pair<Int, Int>? {
        val effectiveMonth = if (monthNum <= 6) 1 else 7
        val lastMonth = monthNum - 1
        return if (lastMonth >= effectiveMonth) effectiveMonth to lastMonth else null
    }

    private fun mspReason(
        currMonth: TimelineMonth,
        current: ParsedPayslip,
    ): String? =
        if (current.earnings.militaryServicePay == 0.0 && currMonth.level?.let { it >= PayLevel.L14 } == true) {
            "Military Service Pay not admissible from Level 14"
        } else {
            null
        }

    private fun tptaReason(
        prevMonth: TimelineMonth,
        currMonth: TimelineMonth,
        timeline: ServiceTimeline,
    ): String? {
        if (TptaAbsenceExplainer.explains(timeline, currMonth.month)) return "Posting change or relocation (Transport Allowance)"
        val prevCity = prevMonth.tptaCity
        val currCity = currMonth.tptaCity
        if (prevCity != null && currCity != null && prevCity != currCity) return "TPTA city class changed ($prevCity→$currCity)"
        val prevDa = prevMonth.daPercent
        val currDa = currMonth.daPercent
        return if (prevDa != null && currDa != null && prevDa != currDa) "TPTA follows DA: $prevDa%→$currDa%" else null
    }

    private fun quartersReason(
        prevMonth: TimelineMonth,
        currMonth: TimelineMonth,
    ): String? {
        if (prevMonth.occupiesQuarters == currMonth.occupiesQuarters) return null
        return if (currMonth.occupiesQuarters) "Government quarters taken" else "Government quarters vacated"
    }

    private fun postingReason(
        kind: PostingKind,
        month: PayMonth,
        timeline: ServiceTimeline,
        label: String,
    ): String? {
        val atEdge =
            timeline.postings.any {
                it.kind == kind && (month.index in (it.first.index - 1)..it.first.index || month.index in it.last.index..(it.last.index + 1))
            }
        return if (atEdge) "$label posting change" else null
    }
}
