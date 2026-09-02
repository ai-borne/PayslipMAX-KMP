package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.subscription.FeatureGate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the D4 gating invariant encoded by [rowMode] — the single point that decides whether a free
 * user navigates into a Premium feature or is shown the upgrade sheet. DSOP_SIMULATOR (Phase 4b) is the
 * worked example: it carries a navigation [PremiumFeatureMeta.target] (RetirementPlanning), so the only
 * thing standing between a free user and the real simulator is that a locked, targeted feature must
 * resolve to [RowMode.LOCKED], never [RowMode.OPENABLE]. If that ever regresses, entitlement leaks.
 */
class PremiumFeaturesRowModeTest {
    private val dsop = featureMeta(FeatureGate.DSOP_SIMULATOR) // AVAILABLE, target = RetirementPlanning
    private val includedGate = featureMeta(FeatureGate.WEALTH_OPTIMIZATION) // AVAILABLE, target = null

    // No production gate is coming-soon today (CLAIM_GENERATOR went AVAILABLE in Phase 4d), so the
    // COMING_SOON row-mode branch is exercised with a synthetic meta rather than a real gate.
    private val comingSoon =
        PremiumFeatureMeta(
            gate = FeatureGate.CLAIM_GENERATOR,
            icon = "🧪",
            title = "Roadmap Feature",
            description = "Not shippable yet",
            target = null,
            availability = PremiumFeatureAvailability.COMING_SOON,
        )

    @Test
    fun lockedTargetedFeatureResolvesToLockedNotOpenable() {
        // The core Phase 4b guarantee: no access => teaser + upgrade sheet, never navigation.
        assertEquals(RowMode.LOCKED, rowMode(dsop, hasAccess = false))
    }

    @Test
    fun unlockedTargetedFeatureIsOpenable() {
        assertEquals(RowMode.OPENABLE, rowMode(dsop, hasAccess = true))
    }

    @Test
    fun unlockedInTabFeatureIsIncluded() {
        assertEquals(RowMode.INCLUDED, rowMode(includedGate, hasAccess = true))
    }

    @Test
    fun lockedInTabFeatureIsStillLocked() {
        assertEquals(RowMode.LOCKED, rowMode(includedGate, hasAccess = false))
    }

    @Test
    fun comingSoonOverridesAccessInBothDirections() {
        // Coming-soon rows are inert regardless of entitlement — never openable, never an upgrade prompt.
        assertEquals(RowMode.COMING_SOON, rowMode(comingSoon, hasAccess = true))
        assertEquals(RowMode.COMING_SOON, rowMode(comingSoon, hasAccess = false))
    }
}
