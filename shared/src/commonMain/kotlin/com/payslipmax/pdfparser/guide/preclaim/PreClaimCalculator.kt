package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Estimates for a move, one outcome per [PreClaimElement] that move shows, in a fixed order, each independent: a missing km hides
 * baggage and conveyance but not the CTG. Never sums them (the portal sets the advance, plan 21 F1), never logs or stores an
 * input, and never reads the network: it is a pure function of the shipped figures and the officer's values.
 */
object PreClaimCalculator {
    fun permanentDuty(
        figures: GuideFigures?,
        request: PermanentDutyRequest,
    ): List<PreClaimOutcome> =
        listOf(
            CtgCalculator.estimate(figures, request.profile, request.ctgCase),
            RoadBaggageCalculator.estimate(figures, request.profile, request.roadKm),
            CarConveyanceCalculator.estimate(figures, request.originState, request.roadKm),
        )

    /**
     * Food, lodging, local taxi and own-car RMA. TR Rule 124 (DA not admissible) makes the first three nil; the RMA is nil without
     * the Rule 40 Note 2 sanction. A bad date range hides the three daily elements but never the RMA, which has no days.
     */
    fun temporaryDuty(
        figures: GuideFigures?,
        request: TemporaryDutyRequest,
    ): TemporaryDutyResult {
        val span = DutyDays.inclusive(request.from, request.to)
        val days = (span as? DutySpan.Days)?.count
        val daily =
            when {
                span is DutySpan.Invalid -> unavailable(span.issue)
                request.trRuleId.isNullOrBlank() -> unavailable(PreClaimIssue.TR_RULE_MISSING)
                request.trRuleId == PreClaimRules.NO_DA_RULE_OPTION_ID -> notPayable()
                else -> dailyEstimates(figures, request, checkNotNull(days))
            }
        val notes = if (days != null && days > PreClaimRules.DA_CAP_DAYS) setOf(PreClaimNote.OVER_180_DAYS) else emptySet()
        return TemporaryDutyResult(days, daily + ownCar(figures, request), notes)
    }

    private val dailyElements = listOf(PreClaimElement.FOOD, PreClaimElement.LODGING, PreClaimElement.LOCAL_TAXI)

    private fun unavailable(issue: PreClaimIssue): List<PreClaimOutcome> = dailyElements.map { Unavailable(it, issue) }

    private fun notPayable(): List<PreClaimOutcome> = dailyElements.map { NotPayable(it, NilReason.RULE_124_NO_DA) }

    private fun dailyEstimates(
        figures: GuideFigures?,
        request: TemporaryDutyRequest,
        days: Int,
    ): List<PreClaimOutcome> =
        listOf(
            TdFoodCalculator.estimate(figures, request.profile, days, request.minutesAway),
            TdLodgingCalculator.estimate(figures, request.profile, days, request.nights, request.nightlyRateRupees),
            TdTaxiCalculator.estimate(figures, request.profile, days),
        )

    /** Own-car RMA is the same table and the same element as the permanent-duty conveyance; here it also needs the sanction. */
    private fun ownCar(
        figures: GuideFigures?,
        request: TemporaryDutyRequest,
    ): PreClaimOutcome =
        when (request.holdsRmaSanction) {
            null -> Unavailable(PreClaimElement.CAR_CONVEYANCE, PreClaimIssue.SANCTION_UNANSWERED)
            false -> NotPayable(PreClaimElement.CAR_CONVEYANCE, NilReason.NO_RULE_40_SANCTION)
            true -> CarConveyanceCalculator.estimate(figures, request.originState, request.roadKm)
        }
}
