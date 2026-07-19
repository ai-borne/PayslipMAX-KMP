package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.EngineResult
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the free-tier "mother" PRO hub (Phase 1 of the Insights PRO consolidation): a single card
 * replacing the three scattered PRO teasers, carrying the same signal each of them used to carry —
 * bundle bullets, the locked anomaly count/labels (never amounts), and the top contextual CTA.
 *
 * The card's content is taller than Robolectric's default test viewport (it lives inside the real
 * screen's LazyColumn in production), so every test wraps it in a scrollable [Column] and scrolls a
 * target into view before asserting/clicking — matching how [InsightsScreenUiTest] drives the real,
 * taller screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InsightsPremiumHubTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private fun record(dsopSubscription: Double = 10_000.0) =
        LedgerRecordEntity(
            dateStr = "02/2026", year = 2026, monthNum = 2, basicPay = 50_000.0, dearnessAllowance = 0.0,
            militaryServicePay = 0.0, transportAllowance = 0.0, transportAllowanceDa = 0.0, houseRentAllowance = 0.0,
            grossPay = 120_000.0, dsopSubscription = dsopSubscription, incomeTax = 10_000.0, netPay = 100_000.0,
        )

    private fun state(
        anomalies: List<Anomaly> = emptyList(),
        totalPotentialTaxSaving: Double = 0.0,
        dsopSubscription: Double = 0.0,
    ) = InsightsState(
        currentRecord = record(dsopSubscription = dsopSubscription),
        previousRecord = record(),
        historySorted = emptyList(),
        engineResult = EngineResult(healthScore = 80, anomalies = anomalies, monthlySavingRate = 8.0, taxRatio = 8.0),
        scoreDelta = null,
        optimizationResult =
            OptimizationResult(
                totalPotentialTaxSaving = totalPotentialTaxSaving,
                marginalRatePct = 0.2,
                regimeAssumed = "OLD",
                opportunities = emptyList(),
                dsopGapMonthly = 0.0,
                dsopCorpusUpliftAtRetirement = 0.0,
            ),
        previousMonthLabel = "January",
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersHubTitleBundleBulletsAndSingleCta() =
        runComposeUiTest {
            setContent {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LockedPremiumHubCard(state = state(), smartInsights = emptyList(), onUpgradeClick = {})
                }
            }

            onNodeWithText(InsightsStrings.premiumHubTitle, substring = true).assertExists()
            // Every catalogued bundle highlight must be listed — locks the SSOT-derived bullet list.
            premiumBundleHighlights().forEach { feature ->
                onNodeWithText(feature.title, substring = true).performScrollTo().assertExists()
            }
            onNodeWithText(InsightsStrings.premiumHubCta).performScrollTo().assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun anomalyHookShowsLockedCountAndCategoryLabelsNeverAmounts() =
        runComposeUiTest {
            val anomaly = Anomaly("MISSING_ALLOWANCE", "field", 123_456.0, "02/2026", "secret leak description")
            setContent {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LockedPremiumHubCard(state = state(anomalies = listOf(anomaly)), smartInsights = emptyList(), onUpgradeClick = {})
                }
            }

            onNodeWithText("1 ${InsightsStrings.advancedAnomaliesLockedCountSuffix}").performScrollTo().assertExists()
            onNodeWithText(InsightsStrings.anomalyLabelMissingAllowance, substring = true).performScrollTo().assertExists()
            // D6: amounts and free-text descriptions must never leak into the locked surface.
            onNodeWithText("123456", substring = true).assertDoesNotExist()
            onNodeWithText("secret leak description").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mostRelevantLineSurfacesTopRecommendedAction() =
        runComposeUiTest {
            setContent {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LockedPremiumHubCard(
                        state = state(totalPotentialTaxSaving = 5_000.0),
                        smartInsights = emptyList(),
                        onUpgradeClick = {},
                    )
                }
            }

            onNodeWithText(
                "${InsightsStrings.premiumHubMostRelevantPrefix}${InsightsStrings.recommendedActionTaxPlannerTitle}",
                substring = true,
            ).performScrollTo().assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun noMostRelevantLineWhenNoRecommendedActionCandidateExists() =
        runComposeUiTest {
            setContent {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LockedPremiumHubCard(state = state(), smartInsights = emptyList(), onUpgradeClick = {})
                }
            }

            onNodeWithText(InsightsStrings.premiumHubMostRelevantPrefix, substring = true).assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingUnlockEverythingInvokesCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LockedPremiumHubCard(state = state(), smartInsights = emptyList(), onUpgradeClick = { clicked = true })
                }
            }

            onNodeWithText(InsightsStrings.premiumHubCta).performScrollTo().performClick()
            waitForIdle()

            assertEquals(true, clicked)
        }

    @Test
    fun bundleHighlightsIsNonEmptySoTheHubAlwaysHasSomethingToShow() {
        assertTrue(premiumBundleHighlights().isNotEmpty())
    }
}
