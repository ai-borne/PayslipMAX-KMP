package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.domain.PersonalFigureResolver
import com.payslipmax.pdfparser.guide.model.FigureModes
import com.payslipmax.pdfparser.guide.model.GuideFigure
import com.payslipmax.pdfparser.guide.model.GuideFigureBand
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * A `da_step` rate for the officer's pay level and DA: the band's base raised by the DA step, kept exact in paise (Rs 50 at DA 60%
 * is 6250). The step is [PersonalFigureResolver.stepPercent], the one DA-step rule; the baggage, food, hotel and taxi lines all
 * come through here, so none of them can round or step differently from another.
 */
internal sealed interface DaStepRate {
    /** [ratePaise] is 0 for a band that carries a [GuideFigureBand.limit] in words and no amount. */
    data class Found(
        val band: GuideFigureBand,
        val ratePaise: Long,
        val source: PreClaimSource,
    ) : DaStepRate

    data class Missing(val issue: PreClaimIssue) : DaStepRate

    companion object {
        private const val PERCENT = 100L

        fun of(
            figures: GuideFigures?,
            figureId: String,
            profile: PreClaimProfile,
            allowLimitBand: Boolean = false,
        ): DaStepRate {
            val figure: GuideFigure =
                figures?.figures?.get(figureId)?.takeIf { it.mode == FigureModes.DA_STEP } ?: return Missing(PreClaimIssue.FIGURE_MISSING)
            val level = profile.level ?: return Missing(PreClaimIssue.LEVEL_MISSING)
            val da = profile.daPercent ?: return Missing(PreClaimIssue.DA_MISSING)
            if (da < 0 || da > PreClaimLimits.MAX_DA_PERCENT) return Missing(PreClaimIssue.DA_OUT_OF_RANGE)
            val band =
                PersonalFigureResolver.bandForLevel(figure, level)?.takeIf { it.base > 0 || (allowLimitBand && it.limit.isNotEmpty()) }
                    ?: return Missing(PreClaimIssue.LEVEL_NOT_COVERED)
            val step = PersonalFigureResolver.stepPercent(figures, da) ?: return Missing(PreClaimIssue.FIGURE_MISSING)
            // base rupees x (100 + step) is the rate in paise: Rs 50 x 125 = 6250 paise.
            val source = PreClaimSource(figureId, figure.effectiveFrom, figure.assumption, da)
            return Found(band, band.base.toLong() * (PERCENT + step), source)
        }
    }
}
