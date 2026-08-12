package com.payslipmax.pdfparser.ui.screens

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.insights.DsopWasteInsight
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.insights.RegimeComparisonResult
import com.payslipmax.pdfparser.insights.RegimeTaxDetail
import com.payslipmax.pdfparser.insights.TaxBlufSummary
import com.payslipmax.pdfparser.insights.TaxStoryNarrative
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner
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

private fun regimeDetail(totalTax: Double) =
    RegimeTaxDetail(
        regimeName = "NEW",
        grossIncome = 4_000_000.0,
        standardDeduction = 75_000.0,
        totalDeductionsAndExemptions = 75_000.0,
        netTaxableIncome = 3_500_000.0,
        baseTax = totalTax,
        surcharge = 0.0,
        cess = 0.0,
        totalTaxPayable = totalTax,
        effectiveTaxRatePct = 18.0,
    )

private fun fullCalculationOptimizationResult() =
    minimalOptimizationResult().copy(
        pcdaOfficialComputation = TaxAndSavings(netTaxableIncome = 3_500_000.0, totalTaxPayable = 629_025.0),
        storyNarrative =
            TaxStoryNarrative(
                financialYear = "2026-27",
                assessmentYear = "2027-28",
                parsedMonthCount = 1,
                coverageHeader = "In Assessment Year 2027-28 (FY 2026-27), we have 1 month of payslips available.",
                missingMonthNudge = null,
                monthlyLedgerList = emptyList(),
                totalTdsYtd = 50_425.0,
                totalDsopYtd = 40_000.0,
                projectedGross = 3_575_000.0,
                projectedTax = 629_025.0,
                effectiveTaxRatePct = 17.6,
            ),
        regimeComparison =
            RegimeComparisonResult(
                financialYear = "2026-27",
                oldRegime = regimeDetail(793_552.0),
                newRegime = regimeDetail(629_025.0),
                winnerRegime = "NEW",
                annualSavings = 164_527.0,
                breakEvenDeduction = 0.0,
            ),
    )

/**
 * Phase 8 view-state coverage: U2 (the active-month indicator the screen previously lacked) and U3
 * (the BLUF summary + collapsed-by-default detail sections).
 */
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun blufSummaryCardRendersHeadlineAndActionLine() =
        runComposeUiTest {
            val bluf =
                TaxBlufSummary(
                    headline = "Your total tax for this financial year will be about ₹6,29,000.",
                    actionLine = "Nothing needs your attention right now.",
                    isActionRequired = false,
                )
            setContent {
                TaxBlufSummaryCard(bluf = bluf)
            }

            onNodeWithText(bluf.headline).assertExists()
            onNodeWithText(bluf.actionLine).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun expandableDetailSectionStartsCollapsedAndExpandsOnClick() =
        runComposeUiTest {
            setContent {
                ExpandableDetailSection(title = "Show details") {
                    Text("hidden detail content")
                }
            }

            onNodeWithText("hidden detail content").assertDoesNotExist()
            onNodeWithText("Show details").performClick()
            onNodeWithText("hidden detail content").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun expandableDetailSectionRendersOptionalSubtitle() =
        runComposeUiTest {
            setContent {
                ExpandableDetailSection(title = "Show details", subtitle = "extra context") {
                    Text("hidden detail content")
                }
            }

            onNodeWithText("extra context").assertExists()
        }

    // Regression guard for the leftover-chevron-clutter bug: PCDA mirror, month-by-month ledger, and
    // the Old-vs-New comparison must sit behind exactly ONE combined disclosure, not one chevron each.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun contentScreenShowsExactlyOneFullCalculationDisclosureCoveringAllThreeDetailCards() =
        runComposeUiTest {
            setContent {
                TaxPlanningContentScreen(
                    optimizationResult = fullCalculationOptimizationResult(),
                    onNavigateBack = {},
                )
            }

            onAllNodesWithText(AppStringsTaxPlanner.expandFullCalculationLabel).assertCountEquals(1)
            onNodeWithText(AppStringsTaxPlanner.pcdaCardTitle).assertDoesNotExist()

            onNodeWithText(AppStringsTaxPlanner.expandFullCalculationLabel).performClick()

            onNodeWithText(AppStringsTaxPlanner.pcdaCardTitle).assertExists()
        }

    // Regression guard for the duplicate-DSOP-card bug: when the DSOP-waste finding fires, its
    // message is already the BLUF flag line -- the full TaxDsopWasteInsightCard must not also render
    // as a second, always-visible copy of the same finding.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dsopWasteCardOnlyAppearsInsideFullCalculationSectionNotAsADuplicateAboveTheFold() =
        runComposeUiTest {
            val dsopWaste =
                DsopWasteInsight(
                    annualDsop = 480_000.0,
                    annualAgif = 150_000.0,
                    cappedContribution = 150_000.0,
                    taxBenefitForgoneAnnual = 45_000.0,
                    message = "Your DSOP + AGIF earns ₹0 benefit under the New Tax Regime.",
                )
            setContent {
                TaxPlanningContentScreen(
                    optimizationResult = fullCalculationOptimizationResult().copy(dsopWasteInsight = dsopWaste),
                    onNavigateBack = {},
                )
            }

            onNodeWithText(AppStringsTaxPlanner.dsopWasteCardTitle).assertDoesNotExist()

            onNodeWithText(AppStringsTaxPlanner.expandFullCalculationLabel).performClick()

            onAllNodesWithText(AppStringsTaxPlanner.dsopWasteCardTitle).assertCountEquals(1)
        }
}
