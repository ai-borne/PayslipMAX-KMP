package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.FigureModes
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Composite Transfer Grant: a percent of basic pay (MoD letter 12630/Mov C/242/D(Mov)/2017, para 3(ii)). The percent, the
 * island percent and the local-move divisor all come from the shipped `ctg` figure; this code holds none. MSP and NPA are not
 * part of the basic, so the profile's basic is the only pay value read.
 */
object CtgCalculator {
    const val FIGURE_ID = "ctg"
    private const val PERCENT_DIVISOR = 100L

    fun estimate(
        figures: GuideFigures?,
        profile: PreClaimProfile,
        case: CtgCase,
    ): PreClaimOutcome {
        fun no(issue: PreClaimIssue) = Unavailable(PreClaimElement.CTG, issue)
        val figure =
            figures?.figures?.get(FIGURE_ID)?.takeIf { it.mode == FigureModes.PERCENT_OF_BASIC && it.percent > 0 }
                ?: return no(PreClaimIssue.FIGURE_MISSING)
        val (percent, divisor) =
            when (case) {
                CtgCase.NORMAL -> figure.percent to 1
                CtgCase.ISLAND -> figure.islandPercent to 1
                CtgCase.LOCAL_MOVE -> figure.percent to figure.localMoveDivisor
            }
        // 0 is how the bundle says "this case is not shipped".
        if (percent <= 0 || divisor <= 0) return no(PreClaimIssue.CASE_NOT_IN_FIGURE)
        val basic = profile.basicPay ?: return no(PreClaimIssue.BASIC_MISSING)
        if (basic < 1 || basic > PreClaimLimits.MAX_BASIC_PAY) return no(PreClaimIssue.BASIC_OUT_OF_RANGE)
        // Half a rupee up, in whole numbers: 2 x numerator + denominator over 2 x denominator.
        val numerator = basic * percent
        val denominator = PERCENT_DIVISOR * divisor
        val rupees = (2 * numerator + denominator) / (2 * denominator)
        val source = PreClaimSource(FIGURE_ID, figure.effectiveFrom, figure.assumption, daPercent = null)
        return CtgEstimate(rupees, percent, divisor, basic, source)
    }
}
