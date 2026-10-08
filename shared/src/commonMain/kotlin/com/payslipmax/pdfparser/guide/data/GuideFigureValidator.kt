package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.GuideBundle

/**
 * Rechecks on every load what `figures.py` enforced when the bundle was built: a figure belongs to a card that exists
 * and carries a personal spec, so a figure can never be drawn on the wrong card. A bundle with no figures is valid.
 * Problems name ids for tests only; they must never be logged or reported.
 */
internal object GuideFigureValidator {
    fun validate(bundle: GuideBundle): List<String> {
        val figures = bundle.figures ?: return emptyList()
        val cards = bundle.cards.associateBy { it.id }
        val problems = mutableListOf<String>()
        for ((key, figure) in figures.figures) {
            val card = cards[figure.card]
            when {
                card == null -> problems += "figure $key: unknown card ${figure.card}"
                card.personal.isEmpty() -> problems += "figure $key: card ${card.id} has no personal spec"
            }
        }
        return problems
    }
}
