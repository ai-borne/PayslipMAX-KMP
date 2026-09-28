package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.subscription.SubscriptionManager
import com.payslipmax.pdfparser.ui.screens.PremiumFeatureAvailability
import com.payslipmax.pdfparser.ui.screens.featureMeta
import com.payslipmax.pdfparser.ui.screens.gateForScreen
import com.payslipmax.pdfparser.ui.screens.premiumBundleHighlights
import com.payslipmax.pdfparser.ui.screens.quickAccessTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PcdaoAuditMonetizationTest {
    @Test
    fun testFeatureGateCatalogMetadata() {
        val meta = featureMeta(FeatureGate.PAYSLIPMAX_AI)
        assertEquals(FeatureGate.PAYSLIPMAX_AI, meta.gate)
        assertEquals(Screen.PcdaoAudit, meta.target)
        assertEquals(PremiumFeatureAvailability.AVAILABLE, meta.availability)
        assertEquals(AppStringsPcdao.screenIcon, meta.icon)
        assertEquals(AppStringsPcdao.screenTitle, meta.title)
        assertEquals(AppStringsPcdao.screenDescription, meta.description)
    }

    @Test
    fun testScreenGateMappingInvariant() {
        assertEquals(FeatureGate.PAYSLIPMAX_AI, gateForScreen(Screen.PcdaoAudit))
        assertTrue(quickAccessTools().any { it.gate == FeatureGate.PAYSLIPMAX_AI && it.target == Screen.PcdaoAudit })
        assertTrue(premiumBundleHighlights().any { it.gate == FeatureGate.PAYSLIPMAX_AI })
    }

    @Test
    fun testSubscriptionManagerGateAccess() {
        var isPremiumEnabled = false
        val manager =
            SubscriptionManager(
                isPremiumEnabledProvider = { isPremiumEnabled },
                isDebugBuildProvider = { true },
                isFreeLaunchModeProvider = { false },
                isTestFlightBuildProvider = { false },
            )

        // Default in debug is FORCE_PRO
        assertTrue(manager.hasAccess(FeatureGate.PAYSLIPMAX_AI))

        // Force free
        manager.setDevOverride(DevOverride.FORCE_FREE)
        assertFalse(manager.hasAccess(FeatureGate.PAYSLIPMAX_AI))

        // Follow flag (free)
        manager.setDevOverride(DevOverride.FOLLOW_FLAG)
        assertFalse(manager.hasAccess(FeatureGate.PAYSLIPMAX_AI))

        // Follow flag (premium enabled)
        isPremiumEnabled = true
        assertTrue(manager.hasAccess(FeatureGate.PAYSLIPMAX_AI))
    }

    @Test
    fun testTeaserStateDerivations() {
        val state =
            PcdaoAuditUiState(
                reconciliationResult = null,
                isLoading = false,
            )
        assertEquals(0.0, state.unclaimedTotal)
        assertEquals(0.0, state.hazardTotal)
        assertEquals(0, state.alarmsCount)
        assertTrue(state.filteredDiscrepancies.isEmpty())
    }
}
