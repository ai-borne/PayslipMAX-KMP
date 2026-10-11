package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.guide.preclaim.CtgCalculator
import com.payslipmax.pdfparser.guide.preclaim.TdFoodCalculator
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuidePreClaim
import com.payslipmax.pdfparser.testing.SyntheticPreClaimWorksheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * "Prepare this claim" appears only on a card that really leads to a worksheet, and the link is derived from data (the LTC rule picker,
 * and the cards of the transfer-grant and food-rate figures), not from a list of card ids kept in code: a card the Guide re-files or
 * a picker option that is re-pointed needs no code change.
 */
class PreClaimRelatedCardsTest {
    private val bundle = SyntheticPreClaimWorksheet.bundle()

    private fun withFigureCards(
        ctg: String,
        food: String,
    ) = bundle.copy(
        figures =
            bundle.figures!!.copy(
                figures =
                    bundle.figures!!.figures +
                        (CtgCalculator.FIGURE_ID to bundle.figures!!.figures.getValue(CtgCalculator.FIGURE_ID).copy(card = ctg)) +
                        (TdFoodCalculator.FIGURE_ID to bundle.figures!!.figures.getValue(TdFoodCalculator.FIGURE_ID).copy(card = food)),
            ),
    )

    @Test
    fun everyCardTheLeavePickerLinksLeadsToTheLeaveTravelWorksheet() {
        val picker = bundle.preclaim!!.options.getValue("ltc_rule").options.map { it.card }
        assertEquals(listOf(SyntheticGuidePreClaim.PICKER_CARD, "RB-T10"), picker)
        picker.forEach { assertEquals(PreClaimMoveType.LEAVE_TRAVEL, PreClaimRelatedCards.moveFor(bundle, it), it) }
    }

    @Test
    fun theTransferGrantCardLeadsToPermanentDutyAndTheFoodRateCardToTemporaryDuty() {
        val figured = withFigureCards(ctg = "RB-T5", food = "RB-T6")
        assertEquals(PreClaimMoveType.PERMANENT_DUTY, PreClaimRelatedCards.moveFor(figured, "RB-T5"))
        assertEquals(PreClaimMoveType.TEMPORARY_DUTY, PreClaimRelatedCards.moveFor(figured, "RB-T6"))
    }

    @Test
    fun anyOtherCardLeadsNowhere() {
        val figured = withFigureCards(ctg = "RB-T5", food = "RB-T6")
        assertNull(PreClaimRelatedCards.moveFor(figured, "RB-T1"))
        assertNull(PreClaimRelatedCards.moveFor(figured, ""))
    }

    @Test
    fun aBundleWithoutAWorksheetOffersNothingEvenOnAFigureCard() {
        val plain = (GuideBundleParser.parse(SyntheticGuideBundle.JSON) as GuideLoadResult.Loaded).bundle
        assertNull(PreClaimRelatedCards.moveFor(plain, SyntheticGuidePreClaim.PICKER_CARD))
        assertNull(PreClaimRelatedCards.moveFor(withFigureCards("RB-T5", "RB-T6").copy(preclaim = null), "RB-T5"))
    }

    @Test
    fun aMoveTheWorksheetDoesNotShipIsNotOffered() {
        val noLeave = bundle.copy(preclaim = bundle.preclaim!!.copy(moves = bundle.preclaim!!.moves - "LTC"))
        assertNull(PreClaimRelatedCards.moveFor(noLeave, SyntheticGuidePreClaim.PICKER_CARD))
    }
}
