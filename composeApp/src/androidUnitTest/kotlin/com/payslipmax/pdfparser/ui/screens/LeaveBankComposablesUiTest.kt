package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.OVER_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The leave table is free on the payslip and the rupee figure is a Premium upsell, so these pin what a user actually sees: the
 * table stays out of the way until asked for, a row reveals its Part II order, the teaser is reachable without expanding, and
 * the Retirement Calculators card shows the summary's figures. Phase 7 item 1 (no UI test for the new composables) closes here.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class LeaveBankComposablesUiTest {
    private val leave =
        LeaveAccumulation(
            listOf(
                accrued("2023-12-31", 30, 271, orderNo = "0446/2023", orderDate = "2024-01-15"),
                ltcEncashed("2024-06-30", 10, orderNo = "0101/2024", orderDate = "2024-07-02"),
                accrued("2024-12-31", 30, OVER_CAP_DAYS, orderNo = "0446/2024", orderDate = "2025-01-15"),
            ),
        )
    private val summary = assertNotNull(LeaveBankCalculator.summarize(listOf(payslip(2025, 12, leave))))
    private val sectionUi = leaveSectionUi(leave, summary, isEditing = false)

    @Test
    fun theSectionStartsCollapsedWithTheTeaserAlreadyVisible() =
        runComposeUiTest {
            setContent { LeaveAccumulationSection(ui = sectionUi, onTeaserClick = {}) }

            onNodeWithText(LeaveBankStrings.sectionTitle).assertExists()
            onNodeWithText("As of 31 Dec 2024 · 301 days").assertExists()
            onNodeWithText(assertNotNull(sectionUi.teaser)).assertExists()
            onNodeWithText(LeaveBankStrings.columnDate).assertDoesNotExist()
        }

    @Test
    fun expandingShowsThisStatementsRowsAndARowTapShowsItsOrder() =
        runComposeUiTest {
            setContent { LeaveAccumulationSection(ui = sectionUi, onTeaserClick = {}) }

            onNodeWithText(LeaveBankStrings.sectionTitle).performClick()
            onNodeWithText("31 Dec 2023").assertExists()
            onNodeWithText("LTC encashed").assertExists()
            onNodeWithText("Pt II Order No 0446/2023 · 15 Jan 2024").assertDoesNotExist()

            onNodeWithText("31 Dec 2023").performClick()
            onNodeWithText("Pt II Order No 0446/2023 · 15 Jan 2024").assertExists()
        }

    @Test
    fun tappingTheTeaserAsksToOpenTheLeaveBankOnce() =
        runComposeUiTest {
            var taps = 0
            setContent { LeaveAccumulationSection(ui = sectionUi, onTeaserClick = { taps++ }) }

            onNodeWithText(assertNotNull(sectionUi.teaser)).performClick()

            assertEquals(1, taps)
        }

    @Test
    fun withoutATeaserTheSectionShowsOnlyTheTable() =
        runComposeUiTest {
            setContent { LeaveAccumulationSection(ui = leaveSectionUi(leave, summary, isEditing = true), onTeaserClick = {}) }

            onNodeWithText(LeaveBankStrings.sectionTitle).assertExists()
            onNodeWithText(assertNotNull(sectionUi.teaser)).assertDoesNotExist()
        }

    @Test
    fun theRetirementCardShowsTheSummaryFiguresAndKeepsItsOrdersCollapsed() =
        runComposeUiTest {
            val card = leaveBankCardUi(summary)
            setContent { LeaveBankCard(ui = card) }

            listOf(card.balance, assertNotNull(card.overCap), card.asOf, card.ltc, card.worth, card.worthBasis).forEach {
                onNodeWithText(it).assertExists()
            }
            onNodeWithText("Pt II Order No 0446/2024 · 15 Jan 2025").assertDoesNotExist()

            onNodeWithText(card.ordersTitle).performClick()
            onNodeWithText("31 Dec 2024").performClick()
            onNodeWithText("Pt II Order No 0446/2024 · 15 Jan 2025").assertExists()
        }
}
