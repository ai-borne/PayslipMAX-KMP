package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.domain.TaxRegime
import com.payslipmax.pdfparser.insights.ArrearsTransparencyInsight
import com.payslipmax.pdfparser.insights.DsopWasteInsight
import com.payslipmax.pdfparser.insights.MidYearRegimeChangeInsight
import com.payslipmax.pdfparser.insights.ReconciliationType
import com.payslipmax.pdfparser.insights.RegimeComparisonResult
import com.payslipmax.pdfparser.insights.RegimeTaxDetail
import com.payslipmax.pdfparser.insights.TaxTrackReconciliation
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

/** Phase 6 view-state coverage: one rendering assertion per new card, per the plan's Phase 6 test list. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaxPhase6CardsTest {
    private fun regimeDetail(
        name: String,
        totalTax: Double,
    ) = RegimeTaxDetail(
        regimeName = name,
        grossIncome = 1_000_000.0,
        standardDeduction = 75_000.0,
        totalDeductionsAndExemptions = 75_000.0,
        netTaxableIncome = 925_000.0,
        baseTax = totalTax - 5_000.0,
        surcharge = 0.0,
        cess = 5_000.0,
        totalTaxPayable = totalTax,
        effectiveTaxRatePct = 10.0,
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun heroCardRendersWinningRegimeAndLiability() =
        runComposeUiTest {
            val comparison =
                RegimeComparisonResult(
                    financialYear = "2025-26",
                    oldRegime = regimeDetail("OLD", 120_000.0),
                    newRegime = regimeDetail("NEW", 95_000.0),
                    winnerRegime = "NEW",
                    annualSavings = 25_000.0,
                    breakEvenDeduction = 0.0,
                )
            setContent {
                TaxLiabilityVerdictHeroCard(
                    regimeComparison = comparison,
                    tdsRunway = null,
                    projectedNextMonthNetPay = null,
                    parsedMonthCount = 6,
                )
            }

            onNodeWithText("₹95,000").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pcdaCardShowsUnavailableNoteWhenTotalTaxPayableIsNull() =
        runComposeUiTest {
            setContent {
                TaxPcdaOfficialComputationCard(
                    taxAndSavings = TaxAndSavings(netTaxableIncome = 800_000.0, totalTaxPayable = null),
                )
            }

            onNodeWithText(
                "PCDA's printed Total Tax Payable was implausible on this payslip and is withheld rather than guessed at.",
            ).assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pcdaCardRendersPrintedTotalTaxPayableWhenAvailable() =
        runComposeUiTest {
            setContent {
                TaxPcdaOfficialComputationCard(
                    taxAndSavings = TaxAndSavings(netTaxableIncome = 800_000.0, totalTaxPayable = 603822.0),
                )
            }

            onNodeWithText("Total Tax Payable: ₹6,03,822").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reconciliationCardRendersMessageAndOptionalWarnings() =
        runComposeUiTest {
            val reconciliation =
                TaxTrackReconciliation(
                    financialYear = "2025-26",
                    tdsTrackAnnual = 700_000.0,
                    tdsTrackRegime = TaxRegime.NEW,
                    liabilityTrackAnnual = 600_000.0,
                    liabilityTrackRegime = TaxRegime.OLD,
                    deltaAmount = 100_000.0,
                    reconciliationType = ReconciliationType.REFUND_EXPECTED,
                    message = "PCDA will deduct ₹7,00,000 under NEW; filing under OLD recovers ₹1,00,000.",
                )
            setContent {
                TaxReconciliationDeltaCard(
                    reconciliation = reconciliation,
                    belatedReturnTrapWarning = "Old Regime is unavailable in a belated return.",
                    midYearRegimeChange = MidYearRegimeChangeInsight(detected = false, regimesSeen = emptyList(), changeMonthNum = null, message = null),
                    arrearsTransparency = ArrearsTransparencyInsight(arrearsAmount = 10_086.0, message = "₹10,086 of arrears was added back verbatim."),
                )
            }

            onNodeWithText(reconciliation.message).assertExists()
            onNodeWithText("Old Regime is unavailable in a belated return.").assertExists()
            onNodeWithText("₹10,086 of arrears was added back verbatim.").assertExists()
            // U1: regime badges must render the full SSOT label, never the raw TaxRegime enum name.
            onNodeWithText("New Tax Regime").assertExists()
            onNodeWithText("Old Tax Regime").assertExists()
            onNodeWithText("NEW", substring = false).assertDoesNotExist()
            onNodeWithText("OLD", substring = false).assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dsopWasteCardRendersForgoneBenefit() =
        runComposeUiTest {
            val insight =
                DsopWasteInsight(
                    annualDsop = 480_000.0,
                    annualAgif = 150_000.0,
                    cappedContribution = 150_000.0,
                    taxBenefitForgoneAnnual = 45_000.0,
                    message = "Your ₹6,30,000/yr DSOP + AGIF contribution earns ₹0 tax benefit under the New Regime.",
                )
            setContent {
                TaxDsopWasteInsightCard(insight = insight)
            }

            onNodeWithText("₹45,000").assertExists()
        }
}
