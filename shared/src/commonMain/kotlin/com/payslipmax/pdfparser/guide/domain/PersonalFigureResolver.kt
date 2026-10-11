package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.model.FigureModes
import com.payslipmax.pdfparser.guide.model.GuideFigure
import com.payslipmax.pdfparser.guide.model.GuideFigureBand
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
    private const val CITY_ANY = "ANY"

    /** HRA / basic is compared to a rate within this many percentage points, for rupee rounding on the payslip. */
    private const val HRA_RATIO_TOLERANCE = 0.1

    fun resolve(
        cardId: String,
        figures: GuideFigures?,
        profile: GuideProfile?,
    ): PersonalFigure? {
        if (figures == null || profile == null) return null
        // A pre-claim figure has no card (empty), so an empty id must never match one.
        val figure = figures.figures.values.firstOrNull { it.card.isNotEmpty() && it.card == cardId } ?: return null
        return when (figure.mode) {
            FigureModes.DA_STEP -> foodRate(figure, figures, profile)
            FigureModes.PERCENT_OF_BASIC -> ctg(figure, profile)
            FigureModes.PLUS_DA -> transport(figure, profile)
            FigureModes.RATE_TABLE -> hra(figure, profile)
            else -> null
        }
    }

    /**
     * The one DA-step rule: a rate rises by `increase_percent` each time DA passes a multiple of `per_da_percent` (MoD 15-09-2017,
     * para 2(iv)). Every escalating rate (the food line, and the pre-claim baggage and TD ceilings) reads it here. Null for a
     * negative DA or a bundle whose period is not above zero, so a caller shows nothing instead of dividing by zero.
     */
    fun stepPercent(
        figures: GuideFigures,
        daPercent: Int,
    ): Int? {
        val step = figures.daStep
        if (step.perDaPercent <= 0 || daPercent < 0) return null
        return step.increasePercent * (daPercent / step.perDaPercent)
    }

    /** The band whose levels list [level] as printed on the payslip, or null when the figure does not cover that level. */
    fun bandForLevel(
        figure: GuideFigure,
        level: String,
    ): GuideFigureBand? = figure.bands.firstOrNull { level in it.levels }

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
        val band = bandForLevel(figure, level) ?: return null
        val stepPercent = stepPercent(figures, da) ?: return null
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
