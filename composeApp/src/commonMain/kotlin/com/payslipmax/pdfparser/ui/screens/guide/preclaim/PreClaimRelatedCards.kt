package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.preclaim.CtgCalculator
import com.payslipmax.pdfparser.guide.preclaim.TdFoodCalculator

/**
 * Which worksheet a Guide card leads to, so the card can offer "Prepare this claim". The link is derived from data that already
 * exists, never from a list of card ids kept here: the LTC rule picker names its cards, and the rule cards behind the transfer grant
 * and the daily food rate are the cards of the permanent- and temporary-duty figures the calculators use.
 */
internal object PreClaimRelatedCards {
    /** The move a card prepares, or null when the card is not related to a worksheet (or the bundle ships none). */
    fun moveFor(
        bundle: GuideBundle,
        cardId: String,
    ): PreClaimMoveType? {
        val section = bundle.preclaim ?: return null
        val figures = bundle.figures?.figures.orEmpty()
        val move =
            when {
                section.options.values.any { group -> group.cardLinked && group.options.any { it.card == cardId } } -> PreClaimMoveType.LEAVE_TRAVEL
                figures[CtgCalculator.FIGURE_ID]?.card == cardId -> PreClaimMoveType.PERMANENT_DUTY
                figures[TdFoodCalculator.FIGURE_ID]?.card == cardId -> PreClaimMoveType.TEMPORARY_DUTY
                else -> null
            }
        return move?.takeIf { it.key in section.moves }
    }
}
