package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.domain.GuideProfile
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideProfileProvider
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.TdSummaryStrings
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The TD summary card on screen (plan 24, P4). Why it matters: it is the one card every officer should read their entitlement off, so each
 * level's hotel, taxi and food amount must be visible without scrolling into a sub-screen, the officer's own row must be marked once and
 * only once, and (owner decision D3) a locked user must still see the table with its authority. The window is tall so "does not exist"
 * means left out, not scrolled off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideTdSummaryUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val profile = GuideProfile(2026, 6, level = "12A", basicPay = 144200.0, daPercent = 60, tptaClass = null, hraAmount = null)
    private val provider = FakeGuideProfileProvider(profile)
    private val guide =
        GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideBundle.withTdSummary())), FakeCrashReporter(), dispatcher, profiles = provider)
    private val search = GuideSearchViewModel(guide, dispatcher)

    private fun open(
        unlocked: Boolean,
        cardId: String = SyntheticGuideBundle.TD_SUMMARY_CARD,
    ) {
        val nav = GuideNavState(listOf(GuideDestination.Card(cardId)))
        composeRule.setContent { GuideTab(nav, GuideAccess(unlocked, onUnlock = {}), guide, search) }
        dispatcher.scheduler.advanceUntilIdle()
        composeRule.waitForIdle()
    }

    private fun shown(text: String) = composeRule.onNodeWithText(text).assertIsDisplayed()

    @Test
    fun everyLevelGroupShowsHotelTaxiAndFoodAtTheDaBand() {
        open(unlocked = true)

        shown("Rates at DA 50 to 99%")
        for (levels in listOf("Levels 9 to 11", "Levels 12 to 13B", "Levels 14 to 18")) shown(levels)
        for (hotel in listOf("Up to ₹2,812.50 a night", "Up to ₹5,625 a night", "Up to ₹9,375 a night")) shown(hotel)
        for (food in listOf("₹1,125 a day", "₹1,250 a day", "₹1,500 a day")) shown(food)
        shown("Non-AC taxi up to ₹422.50 a day")
        shown("AC taxi, up to 50 km a day within the city")
    }

    @Test
    fun foodIsShownByTimeAwayAndCeaAndHostelHaveTheirOwnSection() {
        open(unlocked = true)

        shown("Under 6 hours: ₹337.50")
        shown("6 to 12 hours: ₹787.50")
        shown("Over 12 hours: ₹1,125")
        shown(TdSummaryStrings.ceaHeading)
        shown("₹33,750 a year per child")
        shown("Up to ₹1,01,250 a year per child")
    }

    @Test
    fun aLockedUserStillSeesTheTableAndTheCardsOwnText() {
        open(unlocked = false)

        shown("Levels 12 to 13B")
        shown("Up to ₹5,625 a night")
        shown(GuideStrings.sectionKeyPoints)
    }

    @Test
    fun theOfficersOwnRowIsMarkedOnceAndOnlyOnce() {
        open(unlocked = true)

        composeRule.onAllNodesWithText(TdSummaryStrings.yourLevel).assertCountEquals(1)
    }

    @Test
    fun withoutAPayslipNoRowIsMarkedButTheTableStillShows() {
        provider.current.value = null

        open(unlocked = true)

        composeRule.onAllNodesWithText(TdSummaryStrings.yourLevel).assertCountEquals(0)
        shown("Levels 14 to 18")
    }

    @Test
    fun onlyTheSummaryCardReadsThePayslipsWhileLocked() {
        open(unlocked = false, cardId = "RB-T5")
        assertEquals(0, provider.subscriptions, "an ordinary locked card still never touches the payslips")
    }

    @Test
    fun theSummaryCardReadsThePayslipsEvenWhenLockedBecauseTheTableIsFree() {
        open(unlocked = false)
        assertEquals(1, provider.subscriptions, "owner decision D3: the own-level row is free, so the level is read")
    }
}
