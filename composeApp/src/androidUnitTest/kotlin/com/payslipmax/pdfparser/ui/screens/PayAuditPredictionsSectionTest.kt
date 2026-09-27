package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.DsopRoom
import com.payslipmax.pdfparser.insights.timeline.NextIncrementPrediction
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import kotlin.test.Test

/**
 * [payAuditPredictionsItems] (docs/Plan/09_PayAudit_PhasePlan.md Phase 6, P7-19b): the free "What's next"
 * section (next-increment + DSOP-room cards), untested at the UI layer until now.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class PayAuditPredictionsSectionTest {
    private val upcomingIncrement =
        NextIncrementPrediction(date = PayMonth(2027, 1), predictedBasicPay = 87800.0, level = PayLevel.L11, currentStage = 5, isOverdue = false)
    private val overdueIncrement =
        NextIncrementPrediction(date = PayMonth(2025, 1), predictedBasicPay = 87800.0, level = PayLevel.L11, currentStage = 5, isOverdue = true)
    private val dsopRoom = DsopRoom(financialYearLabel = "FY 2026-27", subscribedYtd = 120000.0, roomLeft = 380000.0)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersTheEmptyStateWithNoPrediction() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditPredictionsItems(incrementPrediction = null, dsopRoom = null) } }

            onNodeWithText("Upload more payslips to predict your next increment.").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersAnUpcomingIncrementAsDueNotOverdue() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditPredictionsItems(incrementPrediction = upcomingIncrement, dsopRoom = null) } }

            onNodeWithText("Due 1/2027", substring = true).assertExists()
            onNodeWithText("Basic Pay moves to ₹87800", substring = true).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersAnOverdueIncrementWithTheOverdueLabel() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditPredictionsItems(incrementPrediction = overdueIncrement, dsopRoom = null) } }

            onNodeWithText("Overdue since 1/2025", substring = true).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersTheDsopRoomCardOnlyWhenNonNull() =
        runComposeUiTest {
            setContent { LazyColumn { payAuditPredictionsItems(incrementPrediction = null, dsopRoom = dsopRoom) } }

            onNodeWithText("Subscribed so far: ₹120000", substring = true).assertExists()
            onNodeWithText("Room left under the ₹5L tax-free cap: ₹380000", substring = true).assertExists()
        }
}
