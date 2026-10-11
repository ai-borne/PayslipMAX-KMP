package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The same screens on the bundle the app ships, so a real label, option or card id that the synthetic worksheet does not have cannot
 * break the UI unseen. The profile is the owner's claim A (level 13A, basic Rs 1,49,000, DA 46%).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class PreClaimRealBundleUiTest : PreClaimUiBase() {
    private val real: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }

    override fun bundle(): GuideBundle = real

    @Test
    fun everyMoveOpensWithItsFirstAndLastPortalFieldAndItsDocuments() {
        show()
        openFromHome("Permanent duty")
        composeRule.onNodeWithText("PCDA A/C No").assertIsDisplayed()
        composeRule.onNodeWithText("From Date").assertIsDisplayed()
        composeRule.onNodeWithText("A copy of the signal, or the proper posting order", substring = true).assertIsDisplayed()
        tap(PreClaimStrings.changeMove)
        tap("Temporary duty")
        composeRule.onNodeWithText("Certificate: no DTS advance taken on the DA part").assertIsDisplayed()
        tap(PreClaimStrings.changeMove)
        tap("Leave travel (LTC)")
        composeRule.onNodeWithText("Total Fare").assertIsDisplayed()
        composeRule.onNodeWithText("The leave sanction certificate", substring = true).assertIsDisplayed()
    }

    @Test
    fun theOwnersVoucherSampleGivesTheTransferGrantAndTheBasicPayCopyValue() {
        show()
        openFromHome("Permanent duty")
        composeRule.onNodeWithText("₹1,19,200").assertIsDisplayed()
        tapDescribed("Copy Basic Pay")
        assertEquals(listOf("149000"), copied)
    }

    @Test
    fun claimAOnTheRealBundleGivesFoodHotelAndTheirCopyValues() {
        show()
        openFromHome("Temporary duty")
        tap(PreClaimStrings.trRule + ": " + PreClaimStrings.trRuleChoose)
        tap("Rule 119 TR")
        setDates(CalendarDate(2024, 2, 1), CalendarDate(2024, 3, 28))
        type(PreClaimStrings.nightlyRate, "4500")
        composeRule.onNodeWithText("₹57,000").assertIsDisplayed()
        composeRule.onNodeWithText("₹2,56,500").assertIsDisplayed()
        tapDescribed("Copy Accommodation Charges")
        assertEquals(listOf("256500"), copied)
    }

    @Test
    fun theLeavePickerOffersAllEightRulesAndTheChosenOneOpensItsRealCard() {
        val nav = show()
        openFromHome("Leave travel (LTC)")
        tap(PreClaimStrings.trRule + ": " + PreClaimStrings.trRuleChoose)
        val labels = real.preclaim!!.options.getValue("ltc_rule").options.map { it.label }
        assertEquals(PICKER_OPTIONS, labels.size)
        labels.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        tap("177(A) for Home Town")
        tap(PreClaimStrings.readRuleCard)
        val card = nav.current as GuideDestination.Card
        assertNotNull(real.cards.firstOrNull { it.id == card.cardId })
    }

    @Test
    fun everyCardTheLeavePickerLinksOffersPrepareThisClaim() {
        real.preclaim!!.options.getValue("ltc_rule").options.map { it.card }.forEach { cardId ->
            assertNotNull(PreClaimRelatedCards.moveFor(real, cardId), cardId)
        }
        show(GuideNavState(listOf(GuideDestination.Card(real.preclaim!!.options.getValue("ltc_rule").options.first().card))))
        composeRule.onNodeWithText(PreClaimStrings.prepareThisClaim).assertIsDisplayed()
    }

    @Test
    fun theTransferGrantAndFoodRateCardsLeadToTheirWorksheets() {
        val ctgCard = real.figures!!.figures.getValue("ctg").card
        val foodCard = real.figures!!.figures.getValue("food_rate").card
        assertEquals(PreClaimMoveType.PERMANENT_DUTY, PreClaimRelatedCards.moveFor(real, ctgCard))
        assertEquals(PreClaimMoveType.TEMPORARY_DUTY, PreClaimRelatedCards.moveFor(real, foodCard))
        show(GuideNavState(listOf(GuideDestination.Card(ctgCard))))
        tap(PreClaimStrings.prepareThisClaim)
        composeRule.onNodeWithText("₹1,19,200").assertIsDisplayed()
    }

    @Test
    fun aCardWithNoWorksheetOffersNoButtonOnTheRealBundle() {
        val other = real.cards.first { PreClaimRelatedCards.moveFor(real, it.id) == null }
        show(GuideNavState(listOf(GuideDestination.Card(other.id))))
        composeRule.onAllNodesWithText(PreClaimStrings.prepareThisClaim).assertCountEquals(0)
    }

    private companion object {
        const val PICKER_OPTIONS = 8
    }
}
