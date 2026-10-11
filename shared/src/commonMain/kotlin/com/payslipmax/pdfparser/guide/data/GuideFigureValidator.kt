package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.FigureModes
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideCard
import com.payslipmax.pdfparser.guide.model.GuideFigure

/**
 * Rechecks on every load what `figures.py` enforced when the bundle was built: a card figure belongs to a card that exists
 * and carries a personal spec, so it can never be drawn on the wrong card; a figure with no card says it is a `preclaim` one;
 * and no band or per-state rate is silently nil. A mode this app version does not know is not checked for shape (the
 * resolver hides it). A bundle with no figures is valid. Problems name ids for tests only; they must never be logged or reported.
 */
internal object GuideFigureValidator {
    private const val USE_PRECLAIM = "preclaim"
    private val BAND_MODES = setOf(FigureModes.DA_STEP, FigureModes.PLUS_DA)
    private const val MIN_LOCAL_MOVE_DIVISOR = 2
    private const val MAX_LOCAL_MOVE_DIVISOR = 10
    private const val MAX_PERCENT = 100

    fun validate(bundle: GuideBundle): List<String> {
        val figures = bundle.figures ?: return emptyList()
        val cards = bundle.cards.associateBy { it.id }
        val problems = mutableListOf<String>()
        for ((key, figure) in figures.figures) {
            problems += cardProblems(key, figure, cards)
            problems += shapeProblems(key, figure)
        }
        return problems
    }

    private fun cardProblems(
        key: String,
        figure: GuideFigure,
        cards: Map<String, GuideCard>,
    ): List<String> {
        if (figure.use == USE_PRECLAIM) {
            return if (figure.card.isEmpty()) emptyList() else listOf("figure $key: a preclaim figure names a card")
        }
        if (figure.card.isEmpty()) return listOf("figure $key: no card and not a preclaim figure")
        val card = cards[figure.card]
        return when {
            card == null -> listOf("figure $key: unknown card ${figure.card}")
            card.personal.isEmpty() -> listOf("figure $key: card ${card.id} has no personal spec")
            else -> emptyList()
        }
    }

    private fun shapeProblems(
        key: String,
        figure: GuideFigure,
    ): List<String> =
        when (figure.mode) {
            in BAND_MODES ->
                if (figure.bands.any { (it.base > 0) == it.limit.isNotEmpty() }) {
                    listOf("figure $key: a band needs a base or a limit, not both or neither")
                } else {
                    emptyList()
                }
            FigureModes.RATE_BY_STATE ->
                if (figure.rates.isEmpty()) {
                    listOf("figure $key: no rates")
                } else {
                    figure.rates.filterValues { it <= 0 }.keys.map { "figure $key: rate for $it must be above zero" }
                }
            FigureModes.PERCENT_OF_BASIC -> ctgCaseProblems(key, figure)
            FigureModes.TIME_SHARE -> timeShareProblems(key, figure)
            else -> emptyList()
        }

    /** The island percent and the local-move divisor decide a rupee amount, so a bad one stops at load. 0 means "not shipped". */
    private fun ctgCaseProblems(
        key: String,
        figure: GuideFigure,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (figure.islandPercent != 0 && figure.islandPercent !in figure.percent..MAX_PERCENT) {
            problems += "figure $key: island_percent must be from percent to $MAX_PERCENT"
        }
        if (figure.localMoveDivisor != 0 && figure.localMoveDivisor !in MIN_LOCAL_MOVE_DIVISOR..MAX_LOCAL_MOVE_DIVISOR) {
            problems += "figure $key: local_move_divisor must be from $MIN_LOCAL_MOVE_DIVISOR to $MAX_LOCAL_MOVE_DIVISOR"
        }
        return problems
    }

    /** The food shares of a same-day trip decide a rupee amount: no gap at the start, no falling share, and the last one is the full day. */
    private fun timeShareProblems(
        key: String,
        figure: GuideFigure,
    ): List<String> {
        val shares = figure.timeShares
        if (shares.isEmpty()) return listOf("figure $key: no time_shares")
        val problems = mutableListOf<String>()
        if (shares.any { it.fromMinutes < 0 || it.percent !in 1..MAX_PERCENT }) {
            problems += "figure $key: each share needs from_minutes of 0 or more and a percent from 1 to $MAX_PERCENT"
        }
        if (shares.first().fromMinutes != 0) problems += "figure $key: the first share must start at from_minutes 0"
        if (shares.zipWithNext().any { (a, b) -> b.fromMinutes <= a.fromMinutes }) problems += "figure $key: from_minutes must strictly ascend"
        if (shares.zipWithNext().any { (a, b) -> b.percent < a.percent }) problems += "figure $key: percent must not fall as the time away grows"
        if (shares.last().percent != MAX_PERCENT) problems += "figure $key: the last share must be the full day (percent $MAX_PERCENT)"
        return problems
    }
}
