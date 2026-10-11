package com.payslipmax.pdfparser.guide.data

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
    private val BAND_MODES = setOf("da_step", "plus_da")
    private const val MODE_RATE_BY_STATE = "rate_by_state"

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
            MODE_RATE_BY_STATE ->
                if (figure.rates.isEmpty()) {
                    listOf("figure $key: no rates")
                } else {
                    figure.rates.filterValues { it <= 0 }.keys.map { "figure $key: rate for $it must be above zero" }
                }
            else -> emptyList()
        }
}
