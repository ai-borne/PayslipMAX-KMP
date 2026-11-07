package com.payslipmax.pdfparser.guide.summary

import com.payslipmax.pdfparser.guide.domain.PersonalFigureResolver
import com.payslipmax.pdfparser.guide.model.GuideFigure
import com.payslipmax.pdfparser.guide.model.GuideFigureBand
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * Works out the TD summary card (plan 24) from the bundle's approved figures. No rupee figure is written here: every amount is a
 * band's base raised by the one DA-step rule, [PersonalFigureResolver.stepPercent], so a base or the step can change in
 * `figures.json` and this follows. Integer arithmetic only, so it is exact on both platforms and needs no iOS timing test.
 *
 * Returns null, so the card shows nothing rather than a guess, when there are no figures, DA is negative, or the hotel figure (whose
 * bands give the rows) is missing. A missing food, taxi, CEA or hostel figure leaves out only its own cell or section.
 */
object TdSummaryBuilder {
    private const val PERCENT = 100L
    private const val HOTEL = "td_hotel"
    private const val TAXI = "td_taxi"
    private const val FOOD = "food_rate"
    private const val FOOD_HOURS = "td_food_hours"
    private const val CEA = "cea"
    private const val HOSTEL = "hostel_subsidy"

    /**
     * The summary at the officer's payslip DA when they have one, otherwise at the bundle's own tier (`table_da_percent`). Null when
     * neither is known, so a table is never drawn at a guessed step.
     */
    fun buildFor(
        figures: GuideFigures?,
        payslipDaPercent: Int?,
    ): TdSummary? {
        val da = payslipDaPercent ?: figures?.daStep?.tableDaPercent?.takeIf { it > 0 } ?: return null
        return build(figures, da)
    }

    fun build(
        figures: GuideFigures?,
        daPercent: Int,
    ): TdSummary? {
        if (figures == null) return null
        val step = PersonalFigureResolver.stepPercent(figures, daPercent) ?: return null
        val hotel = figures.figures[HOTEL]?.takeIf { it.bands.isNotEmpty() } ?: return null
        val timings = figures.figures[FOOD_HOURS]?.timeShares.orEmpty().map { TdFoodTiming(it.fromMinutes, it.percent) }
        val per = figures.daStep.perDaPercent
        val bandFrom = daPercent / per * per
        return TdSummary(
            stepPercent = step,
            daBandFrom = bandFrom,
            daBandTo = bandFrom + per - 1,
            levelGroups = hotel.bands.sortedBy { lowestLevel(it) }.map { group(it, hotel, figures, step, timings) },
            foodTimings = timings,
            ceaPaise = figures.figures[CEA]?.bands?.firstOrNull()?.let { paise(it, step) },
            hostelSubsidyPaise = figures.figures[HOSTEL]?.bands?.firstOrNull()?.let { paise(it, step) },
        )
    }

    private fun group(
        hotelBand: GuideFigureBand,
        hotel: GuideFigure,
        figures: GuideFigures,
        step: Int,
        timings: List<TdFoodTiming>,
    ): TdLevelGroup {
        val level = hotelBand.levels.first()
        val foodPaise = figures.figures[FOOD]?.let { PersonalFigureResolver.bandForLevel(it, level) }?.let { paise(it, step) }
        return TdLevelGroup(
            levels = hotelBand.levels,
            hotelPaise = paise(PersonalFigureResolver.bandForLevel(hotel, level) ?: hotelBand, step),
            taxi = figures.figures[TAXI]?.let { PersonalFigureResolver.bandForLevel(it, level) }?.let { taxi(it, step) },
            foodPaise = foodPaise,
            foodByTimingPaise = if (foodPaise == null) emptyList() else timings.map { foodPaise * it.percent / PERCENT },
        )
    }

    private fun taxi(
        band: GuideFigureBand,
        step: Int,
    ): TdTaxi? = paise(band, step)?.let { TdTaxi.Amount(it) } ?: band.limit.takeIf { it.isNotEmpty() }?.let { TdTaxi.Limit(it) }

    /** The band's base rupees raised by [step] percent, in paise: Rs 50 at a 25% step is 6250. Null for a band that has only a limit text. */
    private fun paise(
        band: GuideFigureBand,
        step: Int,
    ): Long? = band.base.takeIf { it > 0 }?.let { it.toLong() * (PERCENT + step) }

    /** Bands are authored highest level first; the card runs from the lowest. A label with no leading number sorts last. */
    private fun lowestLevel(band: GuideFigureBand): Int = band.levels.first().takeWhile { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
}
