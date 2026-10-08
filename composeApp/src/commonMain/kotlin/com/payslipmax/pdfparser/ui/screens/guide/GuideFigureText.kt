package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.domain.FigureBasis
import com.payslipmax.pdfparser.guide.domain.PersonalFigure
import com.payslipmax.pdfparser.ui.screens.formatCurrency
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/** The three lines of the "Your figure" block: the amount, how it was worked out, and where it came from. */
data class GuideFigureText(
    val headline: String,
    val detail: String,
    val footnote: String,
)

/** All wording is in [GuideStrings]; this only picks the pieces and formats the rupees. Pure, so it is unit-tested. */
fun figureText(figure: PersonalFigure): GuideFigureText {
    val basis = figure.basis
    val footnote = footnote(basis)
    return when (figure) {
        is PersonalFigure.FoodRate ->
            GuideFigureText(
                GuideStrings.figureADay(rupees(figure.amount)),
                GuideStrings.figureFoodDetail(figure.level, rupees(figure.base), figure.stepPercent) + GuideStrings.figureAssumes(basis.assumption),
                footnote,
            )
        is PersonalFigure.Ctg ->
            GuideFigureText(
                GuideStrings.figureAbout(rupees(figure.amount)),
                GuideStrings.figureCtgDetail(figure.percent) + GuideStrings.figureAssumes(basis.assumption),
                footnote,
            )
        is PersonalFigure.Transport ->
            GuideFigureText(
                GuideStrings.figureAMonth(rupees(figure.amount)),
                GuideStrings.figureTransportDetail(figure.level, rupees(figure.base), basis.daPercent ?: 0, cityLabel(figure.cityClass)),
                footnote,
            )
        is PersonalFigure.Hra ->
            GuideFigureText(
                GuideStrings.figurePercentOfBasic(figure.percent),
                GuideStrings.figureHraDetail(figure.hraClass, basis.daPercent ?: 0),
                footnote,
            )
    }
}

private fun rupees(amount: Long): String = formatCurrency(amount.toDouble())

private fun cityLabel(cityClass: String?): String? =
    when (cityClass) {
        "HIGHER" -> GuideStrings.cityHigherRate
        "OTHER" -> GuideStrings.cityOtherPlaces
        else -> null
    }

private fun footnote(basis: FigureBasis): String {
    val month = GuideStrings.yearMonth("${basis.year}-${basis.month.toString().padStart(2, '0')}")
    return GuideStrings.figureFootnote(month, basis.daPercent, GuideStrings.yearMonth(basis.effectiveFrom.take(YEAR_MONTH_LENGTH)))
}

private const val YEAR_MONTH_LENGTH = 7
