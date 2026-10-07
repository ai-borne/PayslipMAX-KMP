package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.subscription.LaunchFlags
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.devOverride
import com.payslipmax.pdfparser.ui.rememberHasAccess

/** What the Guide needs to know about entitlement: whether the paid half is open, and how to offer the upgrade. */
data class GuideAccess(
    val isUnlocked: Boolean,
    val onUnlock: () -> Unit,
)

/**
 * The one decision for the Guide's paid half. While [LaunchFlags.GUIDE_PAYWALL_ENABLED] is off every user reads the
 * whole Guide (owner decision 2026-10-07: no charging for cards still flagged "Unverified point"); once on, only
 * [hasAccess] for [FeatureGate.CLAIM_GUIDE] does. A debug or TestFlight `FORCE_FREE` override still shows the locked
 * screens with the flag off, so QA can see them; the override is inert in production, where it stays `FOLLOW_FLAG`.
 */
fun guideUnlocked(
    hasAccess: Boolean,
    devOverride: DevOverride,
    paywallEnabled: Boolean = LaunchFlags.GUIDE_PAYWALL_ENABLED,
): Boolean = hasAccess || (!paywallEnabled && devOverride != DevOverride.FORCE_FREE)

/** Reactive [guideUnlocked]: recomposes when the premium state or the dev override changes. */
@Composable
fun PayslipViewModel.rememberGuideUnlocked(): Boolean {
    val hasAccess = rememberHasAccess(FeatureGate.CLAIM_GUIDE)
    val override by devOverride.collectAsState()
    return guideUnlocked(hasAccess, override)
}
