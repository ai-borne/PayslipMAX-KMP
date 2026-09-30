package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
@OptIn(ExperimentalTestApi::class)
class MilitaryNomenclatureTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun screenSubtitle_containsCanonicalMilitaryAuditNomenclatureAndZeroTechJargon() {
        assertEquals(
            "PCDA(O) Military Financial Intelligence & Audit Engine",
            AppStringsPcdao.screenSubtitle,
        )
        assertFalse(AppStringsPcdao.screenSubtitle.contains("Moat", ignoreCase = true))
    }

    @Test
    fun publicStrings_containZeroStartupOrInvestorJargon() {
        val forbiddenJargon = listOf("moat", "left on table", "growth hack", "runway", "churn")
        val sampleStrings =
            listOf(
                AppStringsPcdao.screenTitle,
                AppStringsPcdao.screenSubtitle,
                AppStringsPcdao.screenDescription,
                AppStringsPcdao.kpiUnclaimedSubtitle,
                AppStringsPcdao.kpiHazardsSubtitle,
                AppStringsPcdao.kpiAlarmsSubtitle,
                AppStringsPcdao.dashboardBannerTitle,
                AppStringsPcdao.dashboardBannerSubtitleZero,
                AppStringsPcdao.dashboardBannerCta,
                AppStringsPcdao.onboardingSlide1Title,
                AppStringsPcdao.onboardingSlide1Body,
                AppStringsPcdao.onboardingSlide2Title,
                AppStringsPcdao.onboardingSlide2Body,
                AppStringsPcdao.onboardingSlide3Title,
                AppStringsPcdao.onboardingSlide3Body,
                AppStringsPcdao.hazardDialogTitle,
                AppStringsPcdao.hazardDialogBody,
            )

        for (text in sampleStrings) {
            for (jargon in forbiddenJargon) {
                assertFalse(
                    text.contains(jargon, ignoreCase = true),
                    "String '$text' contains prohibited startup/investor jargon '$jargon'",
                )
            }
        }
    }

    @Test
    fun kpiSubtitles_reflectCanonicalMilitaryNomenclature() {
        assertEquals("Annual statutory dues underdrawn", AppStringsPcdao.kpiUnclaimedSubtitle)
        assertTrue(AppStringsPcdao.kpiHazardsSubtitle.contains("18% penal debit"))
        assertTrue(AppStringsPcdao.kpiAlarmsSubtitle.contains("DO2"))
    }

    @Test
    fun dashboardBannerStrings_formatStatementsCorrectlyAcrossCounts() {
        assertEquals(
            AppStringsPcdao.dashboardBannerSubtitleZero,
            AppStringsPcdao.formatDashboardStatementsAnalyzed(0),
        )
        assertEquals(
            AppStringsPcdao.dashboardBannerSubtitleSingle,
            AppStringsPcdao.formatDashboardStatementsAnalyzed(1),
        )
        assertEquals(
            "20 statements analyzed across your IRLA",
            AppStringsPcdao.formatDashboardStatementsAnalyzed(20),
        )
    }

    @Test
    fun onboardingPrimerStrings_containCanonicalMoDCitationsAndTitles() {
        assertEquals("Automated IRLA Statutory Audit", AppStringsPcdao.onboardingSlide1Title)
        assertTrue(AppStringsPcdao.onboardingSlide1Body.contains("Individual Running Ledger Account"))
        assertTrue(AppStringsPcdao.onboardingSlide1Body.contains("7th CPC"))

        assertEquals("Interactive Situational Matrix", AppStringsPcdao.onboardingSlide2Title)
        assertTrue(AppStringsPcdao.onboardingSlide2Body.contains("Mission Presets"))

        assertEquals("1-Tap Official Redressal Kit", AppStringsPcdao.onboardingSlide3Title)
        assertTrue(AppStringsPcdao.onboardingSlide3Body.contains("PCDA(O) Pune"))
        assertTrue(AppStringsPcdao.onboardingSlide3Body.contains("MoD"))
    }

    @Test
    fun situationalMatrixAndHazardStrings_provideClearDemystification() {
        assertTrue(AppStringsPcdao.hazardDialogBody.contains("TR-230(B)"))
        assertTrue(AppStringsPcdao.hazardDialogBody.contains("18% penal interest"))
        assertTrue(AppStringsPcdao.hazardDialogBody.contains("Part II Orders"))
        assertTrue(AppStringsPcdao.presetsSimulationHint.contains("safe, non-destructive"))
    }

    @Test
    fun pcdaoAuditHeader_rendersSanitizedMilitarySubtitleInCompose() =
        runComposeUiTest {
            setContent {
                TopNavBar(onBack = {})
            }

            onNodeWithText(AppStringsPcdao.screenTitle).assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.screenSubtitle).assertIsDisplayed()
            assertEquals(0, onAllNodesWithText("Moat", substring = true).fetchSemanticsNodes().size)
        }
}
