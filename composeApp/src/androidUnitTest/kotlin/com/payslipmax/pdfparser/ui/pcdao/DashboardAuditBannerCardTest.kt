package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardAuditBannerCardTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun pluralizationLogic_formatsAllRangesCorrectly() {
        assertEquals(
            AppStringsPcdao.dashboardBannerSubtitleZero,
            AppStringsPcdao.formatDashboardStatementsAnalyzed(0),
        )
        assertEquals(
            AppStringsPcdao.dashboardBannerSubtitleSingle,
            AppStringsPcdao.formatDashboardStatementsAnalyzed(1),
        )
        assertEquals(
            "2 statements analyzed across your IRLA",
            AppStringsPcdao.formatDashboardStatementsAnalyzed(2),
        )
        assertEquals(
            "20 statements analyzed across your IRLA",
            AppStringsPcdao.formatDashboardStatementsAnalyzed(20),
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun card_displaysTitleSubtitleAndCta_withMultiplePayslips() =
        runComposeUiTest {
            setContent {
                DashboardAuditBannerCard(
                    payslipsCount = 5,
                    onAuditClick = {},
                )
            }

            onNodeWithTag("dashboard_audit_banner_card").assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.dashboardBannerTitle).assertIsDisplayed()
            onNodeWithText("5 statements analyzed across your IRLA").assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.dashboardBannerCta).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun card_displaysZeroPreview_withZeroPayslips() =
        runComposeUiTest {
            setContent {
                DashboardAuditBannerCard(
                    payslipsCount = 0,
                    onAuditClick = {},
                )
            }

            onNodeWithTag("dashboard_audit_banner_card").assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.dashboardBannerSubtitleZero).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun card_displaysSingularSubtitle_withSinglePayslip() =
        runComposeUiTest {
            setContent {
                DashboardAuditBannerCard(
                    payslipsCount = 1,
                    onAuditClick = {},
                )
            }

            onNodeWithTag("dashboard_audit_banner_card").assertIsDisplayed()
            onNodeWithText(AppStringsPcdao.dashboardBannerSubtitleSingle).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickingCtaButton_invokesCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                DashboardAuditBannerCard(
                    payslipsCount = 12,
                    onAuditClick = { clicked = true },
                )
            }

            onNodeWithTag("dashboard_audit_cta").performClick()
            assertTrue(clicked, "Expected onAuditClick callback to be invoked when CTA button is clicked")
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickingCardBody_invokesCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                DashboardAuditBannerCard(
                    payslipsCount = 12,
                    onAuditClick = { clicked = true },
                )
            }

            onNodeWithTag("dashboard_audit_banner_card").performClick()
            assertTrue(clicked, "Expected onAuditClick callback to be invoked when card body is clicked")
        }
}
