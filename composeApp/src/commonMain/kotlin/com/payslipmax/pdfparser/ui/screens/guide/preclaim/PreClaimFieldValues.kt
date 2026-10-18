package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuidePreClaimMove
import com.payslipmax.pdfparser.guide.preclaim.LodgingEstimate
import com.payslipmax.pdfparser.guide.preclaim.NotPayable
import com.payslipmax.pdfparser.guide.preclaim.PerDayEstimate
import com.payslipmax.pdfparser.guide.preclaim.PerKmEstimate
import com.payslipmax.pdfparser.guide.preclaim.PreClaimElement
import com.payslipmax.pdfparser.guide.preclaim.PreClaimOutcome
import com.payslipmax.pdfparser.guide.preclaim.PreClaimProfile

/**
 * The value for each portal box the app can fill, as the plain text the officer would type: a number in rupees or days, or the typed
 * marital status. Only boxes the chosen move has appear, only when the value is known, and a box the rule makes nil (Rule 124 food and
 * lodging, no Rule 40 sanction RMA) holds "0". Ticks, dates and choices carry no copy value: the screen shows them as they are.
 */
internal object PreClaimFieldValues {
    private const val NIL = "0"

    fun of(
        sheet: GuidePreClaimMove?,
        form: PreClaimFormState,
        profile: PreClaimProfile,
        days: Int?,
        outcomes: List<PreClaimOutcome>,
    ): Map<String, String> {
        val ids = sheet?.fields?.map { it.id }?.toSet().orEmpty()
        val byElement = outcomes.associateBy { it.element }
        val values =
            mapOf(
                PreClaimFieldIds.BASIC_PAY to profile.basicPay?.toString(),
                PreClaimFieldIds.NPA to profile.npaAmount?.toString(),
                PreClaimFieldIds.MSP to profile.mspAmount?.toString(),
                PreClaimFieldIds.TOTAL_ABSENCE to days?.toString(),
                PreClaimFieldIds.FOOD_CHARGES to amount(byElement[PreClaimElement.FOOD]),
                PreClaimFieldIds.ACCOMMODATION to amount(byElement[PreClaimElement.LODGING]),
                PreClaimFieldIds.RMA to amount(byElement[PreClaimElement.CAR_CONVEYANCE]),
            )
        return values.filterKeys { it in ids }.mapNotNull { (id, value) -> value?.let { id to it } }.toMap()
    }

    private fun amount(outcome: PreClaimOutcome?): String? =
        when (outcome) {
            is PerDayEstimate -> outcome.rupees.toString()
            is LodgingEstimate -> outcome.rupees.toString()
            is PerKmEstimate -> outcome.rupees.toString()
            is NotPayable -> NIL
            else -> null
        }
}
