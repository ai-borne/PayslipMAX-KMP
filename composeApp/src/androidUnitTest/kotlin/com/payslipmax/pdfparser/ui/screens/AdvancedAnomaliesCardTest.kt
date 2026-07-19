package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.insights.Anomaly
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Locks the PRO-dissolve [AdvancedAnomaliesCard] (Insights PRO consolidation, Phase 2): once a user is
 * PRO, this card shows unlocked findings in full with no lock branch/CTA left — that teaser now lives
 * only in [LockedPremiumHubCard] — and stays entirely absent when there is nothing to show.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdvancedAnomaliesCardTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersUnlockedFindingDescriptionsWhenHasAnomalyDetection() =
        runComposeUiTest {
            val anomaly = Anomaly("MISSING_ALLOWANCE", "field", 5_000.0, "02/2026", "Transport allowance missing")
            setContent {
                AdvancedAnomaliesCard(anomalies = listOf(anomaly), hasAnomalyDetection = true)
            }

            onNodeWithText("Transport allowance missing").assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersNothingWhenNoProTierAnomalies() =
        runComposeUiTest {
            // SALARY_LOSS is FREE-tier (AnomalyTierMap) — it stays free via the health score, never here.
            val anomaly = Anomaly("SALARY_LOSS", "field", 5_000.0, "02/2026", "free-tier finding")
            setContent {
                AdvancedAnomaliesCard(anomalies = listOf(anomaly), hasAnomalyDetection = true)
            }

            onNodeWithText("free-tier finding").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rendersNothingWhenHasAnomalyDetectionIsFalse() =
        runComposeUiTest {
            // Defense in depth: in production this card is only ever reached from the PRO-dissolve
            // branch (isPremium implies hasAnomalyDetection, per SubscriptionManager's binary PRO
            // flag), but the card itself must never leak detail if that invariant is ever violated.
            val anomaly = Anomaly("MISSING_ALLOWANCE", "field", 5_000.0, "02/2026", "should stay hidden")
            setContent {
                AdvancedAnomaliesCard(anomalies = listOf(anomaly), hasAnomalyDetection = false)
            }

            onNodeWithText("should stay hidden").assertDoesNotExist()
        }
}
