package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.subscription.FeatureGate
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression guard for the Smart Insights paywall bypass: a free user could reach TaxPlanning /
 * RetirementPlanning / Representation by tapping an insight card's action button, because
 * [InsightCard] forwarded the click straight to `onActionClick` with no [FeatureGate] check — unlike
 * [RecommendedActions], which already checked `hasAccess` at the click site. This locks the fix: the
 * card's own click handler, not the caller, decides navigate-vs-upgrade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InsightCardGatingTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private fun insight(gate: FeatureGate?) =
        InsightUiModel(
            title = "Tax projection",
            explanation = "explanation",
            severity = InsightSeverity.WARNING,
            actionLabel = "Open Tax Planner",
            actionTarget = Screen.TaxPlanning,
            gate = gate,
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lockedGateOpensUpgradeSheetNotTheTargetScreen() =
        runComposeUiTest {
            var navigated: Screen? = null
            var upgradeShown = false
            setContent {
                InsightCard(
                    insight = insight(gate = FeatureGate.TAX_PLANNER),
                    hasAccess = { false },
                    onActionClick = { navigated = it },
                    onUpgradeClick = { upgradeShown = true },
                )
            }

            onNodeWithText("Open Tax Planner").performClick()

            assertEquals(null, navigated, "a locked gate must never navigate to the target screen")
            assertEquals(true, upgradeShown)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unlockedGateNavigatesAndNeverOpensTheUpgradeSheet() =
        runComposeUiTest {
            var navigated: Screen? = null
            var upgradeShown = false
            setContent {
                InsightCard(
                    insight = insight(gate = FeatureGate.TAX_PLANNER),
                    hasAccess = { true },
                    onActionClick = { navigated = it },
                    onUpgradeClick = { upgradeShown = true },
                )
            }

            onNodeWithText("Open Tax Planner").performClick()

            assertEquals(Screen.TaxPlanning, navigated)
            assertEquals(false, upgradeShown)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun nullGateNavigatesRegardlessOfHasAccess() =
        runComposeUiTest {
            // A card with no gate (e.g. a free finding that happens to carry a target in future) must
            // not be blocked by a `hasAccess` lambda that always denies.
            var navigated: Screen? = null
            setContent {
                InsightCard(
                    insight = insight(gate = null),
                    hasAccess = { false },
                    onActionClick = { navigated = it },
                    onUpgradeClick = {},
                )
            }

            onNodeWithText("Open Tax Planner").performClick()

            assertEquals(Screen.TaxPlanning, navigated)
        }
}
