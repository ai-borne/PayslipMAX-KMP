package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.GuideFigure
import com.payslipmax.pdfparser.guide.model.GuideFigures
import com.payslipmax.pdfparser.guide.model.GuideRateStep
import kotlin.math.abs
import kotlin.math.roundToLong

/** Where a figure came from, so the line can say so: the payslip month, its DA, the rate's date and the assumption named. */
data class FigureBasis(
    val year: Int,
    val month: Int,
    val daPercent: Int?,
    val effectiveFrom: String,
    val assumption: String,
)

/** A resolved personal figure. Held only in the unlocked card state, computed on the device, never sent or logged. */
sealed interface PersonalFigure {
    val basis: FigureBasis

    /** Food charges per full day: [base] raised by [stepPercent] for the DA steps passed. */
    data class FoodRate(val amount: Long, val base: Long, val stepPercent: Int, val level: String, override val basis: FigureBasis) : PersonalFigure

    /** Composite Transfer Grant: [percent] of the latest basic pay. */
    data class Ctg(val amount: Long, val percent: Int, override val basis: FigureBasis) : PersonalFigure

    /** Transport Allowance per month: [base] plus DA on it. [cityClass] is null for the flat level 14+ rate. */
    data class Transport(val amount: Long, val base: Long, val level: String, val cityClass: String?, override val basis: FigureBasis) : PersonalFigure

    /** The HRA rate the payslip's HRA / basic ratio implies: [percent] of basic for city class [hraClass] at this DA. */
    data class Hra(val percent: Int, val hraClass: String, override val basis: FigureBasis) : PersonalFigure
}

/**
 * Turns the bundle's approved rates and the officer's [GuideProfile] into a personal figure for one card. Returns null,
 * so the line is hidden and the card stays complete, whenever an input is missing or not listed on the card: never a
 * guess. Integer arithmetic where the rule is a rate table; plain double only for a percent of basic pay.
 */
object PersonalFigureResolver {
    private const val MODE_DA_STEP = "da_step"
    private const val MODE_PERCENT_OF_BASIC = "percent_of_basic"
    private const val MODE_PLUS_DA = "plus_da"
    private const val MODE_RATE_TABLE = "rate_table"
    private const val CITY_ANY = "ANY"

    /** HRA / basic is compared to a rate within this many percentage points, for rupee rounding on the payslip. */
    private const val HRA_RATIO_TOLERANCE = 0.1

    fun resolve(
        cardId: String,
        figures: GuideFigures?,
        profile: GuideProfile?,
    ): PersonalFigure? {
        if (figures == null || profile == null) return null
        val figure = figures.figures.values.firstOrNull { it.card == cardId } ?: return null
        return when (figure.mode) {
            MODE_DA_STEP -> foodRate(figure, figures, profile)
            MODE_PERCENT_OF_BASIC -> ctg(figure, profile)
            MODE_PLUS_DA -> transport(figure, profile)
            MODE_RATE_TABLE -> hra(figure, profile)
            else -> null
        }
    }

    private fun basis(
        figure: GuideFigure,
        profile: GuideProfile,
        effectiveFrom: String = figure.effectiveFrom,
    ) = FigureBasis(profile.year, profile.month, profile.daPercent, effectiveFrom, figure.assumption)

    private fun foodRate(
        figure: GuideFigure,
        figures: GuideFigures,
        profile: GuideProfile,
    ): PersonalFigure? {
        val level = profile.level ?: return null
        val da = profile.daPercent ?: return null
        val band = figure.bands.firstOrNull { level in it.levels } ?: return null
        val step = figures.daStep
        if (step.perDaPercent <= 0) return null
        val stepPercent = step.increasePercent * (da / step.perDaPercent)
        val amount = (band.base * (100L + stepPercent) + 50L) / 100L
        return PersonalFigure.FoodRate(amount, band.base.toLong(), stepPercent, level, basis(figure, profile))
    }

    private fun ctg(
        figure: GuideFigure,
        profile: GuideProfile,
    ): PersonalFigure? {
        val basic = profile.basicPay?.takeIf { it > 0.0 } ?: return null
        return PersonalFigure.Ctg((basic * figure.percent / 100.0).roundToLong(), figure.percent, basis(figure, profile))
    }

    private fun transport(
        figure: GuideFigure,
        profile: GuideProfile,
    ): PersonalFigure? {
        val level = profile.level ?: return null
        val da = profile.daPercent ?: return null
        val band =
            figure.bands.firstOrNull { level in it.levels && (it.cityClass == CITY_ANY || it.cityClass == profile.tptaClass) } ?: return null
        val amount = (band.base * (100L + da) + 50L) / 100L
        return PersonalFigure.Transport(amount, band.base.toLong(), level, band.cityClass.takeIf { it != CITY_ANY }, basis(figure, profile))
    }

    private fun hra(
        figure: GuideFigure,
        profile: GuideProfile,
    ): PersonalFigure? {
        val basic = profile.basicPay?.takeIf { it > 0.0 } ?: return null
        val hra = profile.hraAmount?.takeIf { it > 0.0 } ?: return null
        val da = profile.daPercent ?: return null
        val ratio = hra / basic * 100.0
        for ((name, steps) in figure.classes) {
            // The rate in force for this class at the payslip's DA. A ratio equal to another step's rate is a mismatch
            // (Pay Audit's finding), so it matches nothing here and the line stays hidden.
            val step: GuideRateStep = steps.lastOrNull { it.fromDaPercent <= da } ?: continue
            if (abs(ratio - step.percent) <= HRA_RATIO_TOLERANCE) {
                return PersonalFigure.Hra(step.percent, name, basis(figure, profile, step.effectiveFrom))
            }
        }
        return null
    }
}
