package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pcdao.timeline.CareerMilestone
import com.payslipmax.pcdao.timeline.MilestoneType
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CareerMilestonesCardTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private fun createMilestone(
        title: String,
        monthName: String = "JUL",
        year: Int = 2026,
        type: MilestoneType = MilestoneType.ANNUAL_INCREMENT_VERIFIED,
    ): CareerMilestone =
        CareerMilestone(
            type = type,
            dateStr = "$year-07-01",
            monthName = monthName,
            year = year,
            title = title,
            description = "Test description for $title",
            monetaryImpact = 2500.0,
            statutoryAuthority = "Army Officers Pay Rules 2017, Rule 10",
            isAlert = false,
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun emptyMilestonesList_rendersNothing() =
        runComposeUiTest {
            setContent {
                CareerMilestonesCard(milestones = emptyList())
            }

            onAllNodesWithText(AppStringsPcdao.careerMilestoneTitle).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun singleMilestone_displaysMilestoneWithoutToggle() =
        runComposeUiTest {
            val milestone = createMilestone("Annual Increment Verified (JUL 2026)")
            setContent {
                CareerMilestonesCard(milestones = listOf(milestone))
            }

            onNodeWithText(AppStringsPcdao.careerMilestoneTitle).assertIsDisplayed()
            onNodeWithText("Annual Increment Verified (JUL 2026)").assertIsDisplayed()
            onAllNodesWithText(AppStringsPcdao.milestoneShowLess).assertCountEquals(0)
            onAllNodesWithText(AppStringsPcdao.formatMilestoneShowAll(1)).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun multipleMilestones_defaultsToCollapsedWithFirstMilestoneAndShowAllButton() =
        runComposeUiTest {
            val m1 = createMilestone("Annual Increment Verified (JUL 2026)")
            val m2 = createMilestone("DA Revision Adjustment (JAN 2026)", monthName = "JAN")
            val m3 = createMilestone("Annual Increment Verified (JUL 2025)", year = 2025)

            setContent {
                CareerMilestonesCard(milestones = listOf(m1, m2, m3))
            }

            onNodeWithText(AppStringsPcdao.careerMilestoneTitle).assertIsDisplayed()
            onNodeWithText("Annual Increment Verified (JUL 2026)").assertIsDisplayed()
            onAllNodesWithText("DA Revision Adjustment (JAN 2026)").assertCountEquals(0)
            onAllNodesWithText("Annual Increment Verified (JUL 2025)").assertCountEquals(0)

            val showAllText = AppStringsPcdao.formatMilestoneShowAll(3)
            onNodeWithText(showAllText).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickingShowAll_expandsAllMilestonesAndShowsRecentOnlyButton() =
        runComposeUiTest {
            val m1 = createMilestone("Annual Increment Verified (JUL 2026)")
            val m2 = createMilestone("DA Revision Adjustment (JAN 2026)", monthName = "JAN")
            val m3 = createMilestone("Annual Increment Verified (JUL 2025)", year = 2025)

            setContent {
                CareerMilestonesCard(milestones = listOf(m1, m2, m3))
            }

            val showAllText = AppStringsPcdao.formatMilestoneShowAll(3)
            onNodeWithText(showAllText).performClick()

            onNodeWithText("Annual Increment Verified (JUL 2026)").assertIsDisplayed()
            onNodeWithText("DA Revision Adjustment (JAN 2026)").assertIsDisplayed()
            onNodeWithText("Annual Increment Verified (JUL 2025)").assertIsDisplayed()

            onNodeWithText(AppStringsPcdao.milestoneShowLess).assertIsDisplayed()
            onAllNodesWithText(showAllText).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickingShowRecentOnly_collapsesBackToFirstMilestone() =
        runComposeUiTest {
            val m1 = createMilestone("Annual Increment Verified (JUL 2026)")
            val m2 = createMilestone("DA Revision Adjustment (JAN 2026)", monthName = "JAN")
            val m3 = createMilestone("Annual Increment Verified (JUL 2025)", year = 2025)

            setContent {
                CareerMilestonesCard(milestones = listOf(m1, m2, m3))
            }

            val showAllText = AppStringsPcdao.formatMilestoneShowAll(3)
            onNodeWithText(showAllText).performClick()
            onNodeWithText("DA Revision Adjustment (JAN 2026)").assertIsDisplayed()

            onNodeWithText(AppStringsPcdao.milestoneShowLess).performClick()

            onNodeWithText("Annual Increment Verified (JUL 2026)").assertIsDisplayed()
            onAllNodesWithText("DA Revision Adjustment (JAN 2026)").assertCountEquals(0)
            onAllNodesWithText("Annual Increment Verified (JUL 2025)").assertCountEquals(0)
            onNodeWithText(showAllText).assertIsDisplayed()
        }
}
