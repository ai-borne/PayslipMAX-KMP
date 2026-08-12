package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.OptimizationResult
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

private fun minimalOptimizationResult() =
    OptimizationResult(
        totalPotentialTaxSaving = 0.0,
        marginalRatePct = 0.0,
        regimeAssumed = "OLD",
        opportunities = emptyList(),
        dsopGapMonthly = 0.0,
        dsopCorpusUpliftAtRetirement = 0.0,
    )

/** Phase 8 view-state coverage: U2, the active-month indicator the Tax Planner screen previously lacked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaxPhase8CardsTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dataAsOfBannerRendersActivePayslipMonthAndYear() =
        runComposeUiTest {
            setContent {
                TaxDataAsOfBanner(activePayslipLabel = "April 2026")
            }

            onNodeWithText("Data as of: April 2026").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun contentScreenRendersBannerWhenOptimizationResultAndLabelBothPresent() =
        runComposeUiTest {
            setContent {
                TaxPlanningContentScreen(
                    optimizationResult = minimalOptimizationResult(),
                    activePayslipLabel = "April 2026",
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Data as of: April 2026").assertExists()
        }

    // No tax data to be "as of" a month for yet -- showing the banner over the empty state would
    // imply a month was parsed when nothing has been.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun contentScreenOmitsBannerWhenOptimizationResultIsNull() =
        runComposeUiTest {
            setContent {
                TaxPlanningContentScreen(
                    optimizationResult = null,
                    activePayslipLabel = "April 2026",
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Data as of: April 2026").assertDoesNotExist()
        }
}
