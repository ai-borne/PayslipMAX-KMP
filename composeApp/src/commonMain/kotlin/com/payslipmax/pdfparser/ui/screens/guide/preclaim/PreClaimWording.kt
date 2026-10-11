package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.preclaim.CtgEstimate
import com.payslipmax.pdfparser.guide.preclaim.LimitNote
import com.payslipmax.pdfparser.guide.preclaim.LodgingEstimate
import com.payslipmax.pdfparser.guide.preclaim.NotPayable
import com.payslipmax.pdfparser.guide.preclaim.PerDayEstimate
import com.payslipmax.pdfparser.guide.preclaim.PerKmEstimate
import com.payslipmax.pdfparser.guide.preclaim.PreClaimOutcome
import com.payslipmax.pdfparser.guide.preclaim.PreClaimSource
import com.payslipmax.pdfparser.guide.preclaim.Unavailable
import com.payslipmax.pdfparser.ui.screens.formatCurrency
import com.payslipmax.pdfparser.ui.theme.PreClaimIssueStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/**
 * One estimate row as the screen draws it. [amount] is null when there is nothing to show as a sum (a missing input, a rule given in
 * words); [source] is the "Estimate. Rule: ... As of ..." line, null when the row carries no rate (nil by rule, or a gap).
 */
data class PreClaimEstimateLine(
    val title: String,
    val amount: String?,
    val detail: String,
    val source: String?,
)

/** Turns the calculators' outcomes into words. Pure: every sentence is in [PreClaimStrings] or [PreClaimIssueStrings]. */
internal object PreClaimWording {
    private const val PAISE = 100L
    private const val PAISE_DIGITS = 2

    fun line(outcome: PreClaimOutcome): PreClaimEstimateLine {
        val title = PreClaimStrings.element(outcome.element)
        return when (outcome) {
            is CtgEstimate -> PreClaimEstimateLine(title, rupees(outcome.rupees), PreClaimStrings.ctgDetail(outcome.percentOfBasic, rupees(outcome.basicPay), outcome.divisor), source(outcome.source))
            is PerKmEstimate -> PreClaimEstimateLine(title, rupees(outcome.rupees), PreClaimStrings.perKmDetail(outcome.km, rate(outcome.ratePaise)), source(outcome.source))
            is PerDayEstimate ->
                PreClaimEstimateLine(title, rupees(outcome.rupees), PreClaimStrings.perDayDetail(outcome.days, rate(outcome.ratePaise), outcome.sharePercent), source(outcome.source))
            is LodgingEstimate ->
                PreClaimEstimateLine(
                    title,
                    rupees(outcome.rupees),
                    PreClaimStrings.lodgingDetail(outcome.nights, rupees(outcome.claimedPerNight), rate(outcome.ceilingPaise), outcome.capped),
                    source(outcome.source),
                )
            is LimitNote -> PreClaimEstimateLine(title, null, PreClaimStrings.limitNote(outcome.text), source(outcome.source))
            is NotPayable -> PreClaimEstimateLine(title, PreClaimStrings.nilAmount, PreClaimStrings.nilReason(outcome.reason), null)
            is Unavailable -> PreClaimEstimateLine(title, null, PreClaimIssueStrings.of(outcome.issue), null)
        }
    }

    fun rupees(amount: Long): String = formatCurrency(amount.toDouble())

    /** A rate kept in paise, with its paise when it has any: 6250 is "₹62.50", 45000 is "₹450". Never rounded, because a ceiling is exact. */
    fun rate(paise: Long): String {
        val whole = rupees(paise / PAISE)
        val rest = paise % PAISE
        return if (rest == 0L) whole else "$whole.${rest.toString().padStart(PAISE_DIGITS, '0')}"
    }

    private fun source(source: PreClaimSource): String = PreClaimStrings.estimateSource(source.assumption, source.effectiveFrom)
}
