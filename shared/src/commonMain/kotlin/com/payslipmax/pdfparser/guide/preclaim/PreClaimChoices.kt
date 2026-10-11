package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.guide.model.FigureModes
import com.payslipmax.pdfparser.guide.model.GuideFigures

/**
 * The values a worksheet picker may offer, read from the shipped figures so a picker can only hold what a calculator accepts: levels
 * exactly as the bands print them, states exactly as the circular spells them (a near miss is refused, never priced at nil).
 */
object PreClaimChoices {
    /** Every pay level a pre-claim rate band covers, lowest first (9, 10, 10A, 10B, 11, ...), each once. Empty with no figures. */
    fun levels(figures: GuideFigures?): List<String> =
        figures
            ?.figures
            ?.values
            .orEmpty()
            .filter { it.use == FigureModes.USE_PRECLAIM && it.mode == FigureModes.DA_STEP }
            .flatMap { figure -> figure.bands.flatMap { it.levels } }
            .distinct()
            .sortedWith(compareBy({ it.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE }, { it }))

    /** The states of the RMA table in the bundle's own spelling, alphabetical. Empty with no table. */
    fun states(figures: GuideFigures?): List<String> =
        figures?.figures?.get(CarConveyanceCalculator.FIGURE_ID)?.takeIf { it.mode == FigureModes.RATE_BY_STATE }?.rates?.keys.orEmpty().sorted()
}
