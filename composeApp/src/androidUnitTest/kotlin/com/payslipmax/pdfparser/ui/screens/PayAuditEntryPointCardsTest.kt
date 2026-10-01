package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.ui.theme.PayAuditEntryStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pay Audit entry points ported from pay_audit_1.0 (consolidation plan Phase 3). Both cards must be
 * discoverable without any claim about findings: the Pay Audit principle is that only the audit screen,
 * backed by the payslips, may say a month is right or wrong.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalTestApi::class)
class PayAuditEntryPointCardsTest {
    @Test
    fun dashboardBannerNamesTheFeatureAndHowManyPayslipsItWillCheck() =
        runComposeUiTest {
            setContent { DashboardAuditBannerCard(payslipsCount = 5, onAuditClick = {}) }

            onNodeWithTag("dashboard_audit_banner_card").assertIsDisplayed()
            onNodeWithText(PayAuditEntryStrings.dashboardBannerTitle).assertIsDisplayed()
            onNodeWithText("Check your 5 payslips against the rules").assertIsDisplayed()
            onNodeWithText(PayAuditEntryStrings.dashboardBannerCta).assertIsDisplayed()
        }

    @Test
    fun dashboardBannerSubtitleIsSingularForOnePayslip() {
        assertEquals("Check your 1 payslip against the rules", PayAuditEntryStrings.dashboardBannerSubtitle(1))
        assertEquals("Check your 12 payslips against the rules", PayAuditEntryStrings.dashboardBannerSubtitle(12))
    }

    @Test
    fun dashboardBannerNeverClaimsAFindingOrAnAmount() {
        val copy = listOf(PayAuditEntryStrings.dashboardBannerTitle, PayAuditEntryStrings.dashboardBannerSubtitle(5), PayAuditEntryStrings.dashboardBannerCta)
        copy.forEach { text ->
            assertEquals(false, text.contains("₹") || text.contains("unclaimed", ignoreCase = true) || text.contains("owed", ignoreCase = true), text)
        }
    }

    @Test
    fun tappingTheBannerBodyOrItsButtonOpensPayAudit() =
        runComposeUiTest {
            var opened = 0
            setContent { DashboardAuditBannerCard(payslipsCount = 3, onAuditClick = { opened++ }) }

            onNodeWithTag("dashboard_audit_cta").performClick()
            onNodeWithTag("dashboard_audit_banner_card").performClick()

            assertEquals(2, opened)
        }

    @Test
    fun replicaCardOffersToAuditTheMonthInView() =
        runComposeUiTest {
            setContent { ReplicaAuditActionCard(onAuditClick = {}) }

            onNodeWithTag("replica_audit_action_card").assertIsDisplayed()
            onNodeWithText(PayAuditEntryStrings.replicaCtaTitle).assertIsDisplayed()
            onNodeWithText(PayAuditEntryStrings.replicaCtaBody).assertIsDisplayed()
        }

    @Test
    fun tappingTheReplicaCardInvokesTheCallbackOnce() =
        runComposeUiTest {
            var opened = 0
            setContent { ReplicaAuditActionCard(onAuditClick = { opened++ }) }

            onNodeWithTag("replica_audit_action_card").performClick()

            assertEquals(1, opened)
        }
}
