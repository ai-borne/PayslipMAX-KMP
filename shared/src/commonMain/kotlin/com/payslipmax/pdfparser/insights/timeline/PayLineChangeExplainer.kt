package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.PayAuditWording
import kotlin.math.abs

/**
 * Explains a month-to-month move in the pay-line fields the [ServiceTimeline] already models (Phases 1-2
 * of docs/Plan/09_PayAudit_PhasePlan.md): Basic Pay (DNI/promotion), DA and its arrears (rate revision),
 * Transport Allowance (posting change, relocation, or DA), its posting-change arrears, HRA/licence fee
 * (quarters), Risk & Hardship/Field allowance (posting spans), and Non-Practicing Allowance (a fixed
 * percentage of basic pay). Fields the timeline does not model (income tax, DSOP, one-off adjustments,
 * and allowances whose eligibility this app doesn't parse — CEA, dress/ration, technical, special forces
 * pay) are out of scope — a changed field with no matching rule below comes back with
 * [ChangeExplanation.reason] null rather than a guess, so coverage can be measured honestly.
 */
object PayLineChangeExplainer {
    private const val AMOUNT_TOLERANCE = 0.5
    private const val DA_RATE_CHANGED = "Dearness Allowance (DA) rate changed from"
    private const val ARREARS_DROPPED = "Arrears were paid last month and do not repeat"

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
            Triple("arrearsTpta", previous.earnings.arrearsTpta, current.earnings.arrearsTpta),
            Triple("militaryServicePay", previous.earnings.militaryServicePay, current.earnings.militaryServicePay),
            Triple("transportAllowance", tptaTotal(previous), tptaTotal(current)),
            Triple("houseRentAllowance", previous.earnings.houseRentAllowance, current.earnings.houseRentAllowance),
            Triple("licenseFee", previous.deductions.licenseFee, current.deductions.licenseFee),
            Triple("riskHardshipAllowance", previous.earnings.riskHardshipAllowance, current.earnings.riskHardshipAllowance),
            Triple("fieldAllowance", previous.earnings.fieldAllowance, current.earnings.fieldAllowance),
            Triple("nonPracticingAllowance", previous.earnings.nonPracticingAllowance, current.earnings.nonPracticingAllowance),
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
            "arrearsTpta" -> tptaArrearsReason(from, to, currMonth, timeline)
            "militaryServicePay" -> mspReason(currMonth, current)
            "transportAllowance" -> tptaReason(from, to, prevMonth, currMonth, timeline)
            "houseRentAllowance", "licenseFee" -> quartersReason(prevMonth, currMonth)
            "riskHardshipAllowance" -> postingReason(PostingKind.RISK_HARDSHIP, currMonth.month, timeline, "Risk & Hardship")
            "fieldAllowance" -> postingReason(PostingKind.FIELD, currMonth.month, timeline, "Field")
            "nonPracticingAllowance" -> npaReason(from, currMonth, timeline)
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
        if (prevDa != null && currDa != null && prevDa != currDa) return "$DA_RATE_CHANGED ${PayAuditWording.percentChange(prevDa, currDa)}"
        val payBaseEvent = timeline.events.firstOrNull { it.month == currMonth.month } ?: return null
        return "Dearness Allowance (DA) is a percentage of your pay, which went up with your ${payBaseEvent.type.cause()}"
    }

    /**
     * DA (or separately-printed TPTA-DA) arrears are paid the month a rise first appears, for the months
     * since its effective date, and never repeat the month after — a one-off arrears payment, not a new rate.
     */
    private fun arrearsReason(
        from: Double,
        to: Double,
        prevMonth: TimelineMonth,
        currMonth: TimelineMonth,
        current: ParsedPayslip,
        tptaDaSeparate: Boolean,
    ): String? {
        if (to == 0.0 && from > 0.0) return ARREARS_DROPPED
        val prevDa = prevMonth.daPercent
        val currDa = currMonth.daPercent
        if (prevDa == null || currDa == null || currDa <= prevDa) return null
        if (tptaDaSeparate && current.earnings.transportAllowanceDa <= 0.0) return null
        val (rangeFrom, rangeTo) = arrearsRange(current.monthNum) ?: return null
        val arrearsPhrase = if (tptaDaSeparate) "the arrears of DA on Transport Allowance" else "the arrears"
        return "$DA_RATE_CHANGED ${PayAuditWording.percentChange(prevDa, currDa)}; these are $arrearsPhrase for ${PayAuditWording.monthSpan(rangeFrom, rangeTo, current.year)}"
    }

    private fun TimelineEventType.cause(): String = if (this == TimelineEventType.PROMOTION) "promotion" else "annual increment"

    /** Months from the rise's effective date (1 Jan or 1 Jul) up to the month before this payslip; mirrors DaArrearsAuditor. */
    private fun arrearsRange(monthNum: Int): Pair<Int, Int>? {
        val effectiveMonth = if (monthNum <= 6) 1 else 7
        val lastMonth = monthNum - 1
        return if (lastMonth >= effectiveMonth) effectiveMonth to lastMonth else null
    }

    /**
     * Base TPTA arrears (`arrearsTpta`) are a posting-change arrears payment, not a DA-rate rise — distinct
     * from [arrearsReason], which only fires on a DA revision. Triggers when the same posting-change or
     * relocation edge that explains a TPTA drop/absence ([TptaAbsenceExplainer]) sits at this month.
     */
    private fun tptaArrearsReason(
        from: Double,
        to: Double,
        currMonth: TimelineMonth,
        timeline: ServiceTimeline,
    ): String? {
        if (to == 0.0 && from > 0.0) return ARREARS_DROPPED
        return if (TptaAbsenceExplainer.explains(timeline, currMonth.month)) {
            "Arrears of Transport Allowance after a posting change or relocation"
        } else {
            null
        }
    }

    private fun mspReason(
        currMonth: TimelineMonth,
        current: ParsedPayslip,
    ): String? =
        if (current.earnings.militaryServicePay == 0.0 && currMonth.level?.let { it >= PayLevel.L14 } == true) {
            "Military Service Pay is not paid from Level 14"
        } else {
            null
        }

    private fun tptaReason(
        from: Double,
        to: Double,
        prevMonth: TimelineMonth,
        currMonth: TimelineMonth,
        timeline: ServiceTimeline,
    ): String? {
        val prevDa = prevMonth.daPercent
        val currDa = currMonth.daPercent
        val daChanged = prevDa != null && currDa != null && prevDa != currDa
        // TPTA that moved exactly in proportion to DA is DA-linked even in a posting-change month.
        if (daChanged && abs(to - from * (100 + currDa!!) / (100 + prevDa!!)) <= AMOUNT_TOLERANCE) return tptaFollowsDa(prevDa, currDa)
        if (TptaAbsenceExplainer.explains(timeline, currMonth.month)) return "Posting change or relocation (Transport Allowance)"
        val prevCity = prevMonth.tptaCity
        val currCity = currMonth.tptaCity
        if (prevCity != null && currCity != null && prevCity != currCity) return "Transport Allowance city class changed from ${cityLabel(prevCity)} to ${cityLabel(currCity)}"
        return if (daChanged) tptaFollowsDa(prevDa!!, currDa!!) else null
    }

    private fun tptaFollowsDa(
        prevDa: Int,
        currDa: Int,
    ) = "Transport Allowance (TPTA) moves with DA, which changed from ${PayAuditWording.percentChange(prevDa, currDa)}"

    private fun cityLabel(city: TptaCityClass) = if (city == TptaCityClass.HIGHER) "higher-rate cities" else "other cities"

    /**
     * NPA is a fixed 20% of basic pay, capped at basic+MSP+NPA ≤ 237,500 (GoI MoD letter dated 28-09-2017;
     * Handbook of Pay and Allowances 2023, p. 104), so it only moves when basic pay itself does. Its
     * onset/cessation (medical-corps eligibility) is not modeled anywhere in the app, so a month with no
     * prior NPA is left unexplained rather than guessed.
     */
    private fun npaReason(
        from: Double,
        currMonth: TimelineMonth,
        timeline: ServiceTimeline,
    ): String? {
        if (from <= 0.0) return null
        val payBaseEvent = timeline.events.firstOrNull { it.month == currMonth.month } ?: return null
        return "Non-Practicing Allowance (NPA) is a percentage of your basic pay, which went up with your ${payBaseEvent.type.cause()}"
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
