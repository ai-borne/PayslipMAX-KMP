package com.payslipmax.pdfparser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * ViewModel façade over [com.payslipmax.pdfparser.subscription.SubscriptionManager] so composables
 * never reach into the manager directly. [hasAccess] is the single gating question every PRO surface
 * asks; [devOverride]/[setDevOverride] back the debug-only override control.
 */
fun PayslipViewModel.hasAccess(gate: FeatureGate): Boolean = subscriptionManager.hasAccess(gate)

val PayslipViewModel.devOverride: StateFlow<DevOverride>
    get() = subscriptionManager.devOverride

fun PayslipViewModel.setDevOverride(override: DevOverride) = subscriptionManager.setDevOverride(override)

/**
 * The same verdict as [hasAccess], as a flow, for work that outlives every screen (the Part II reminder sync). It is re-asked whenever one
 * of the manager's three inputs changes (the premium flag, the dev override, the billing state) and emits only when the answer changes.
 */
fun PayslipViewModel.hasAccessFlow(gate: FeatureGate): Flow<Boolean> =
    combine(uiState, devOverride, billingManager.subscriptionState) { _, _, _ -> hasAccess(gate) }.distinctUntilChanged()

/**
 * Reactive gate read for Compose: observes both the premium flag and the dev override so the caller
 * recomposes when either changes, then returns the current [hasAccess] verdict for [gate].
 */
@Composable
fun PayslipViewModel.rememberHasAccess(gate: FeatureGate): Boolean {
    val premiumEnabled by uiState.collectAsState()
    val currentOverride by devOverride.collectAsState()
    // Keys are read here so Compose recomposes when either input changes; hasAccess() stays the SSOT.
    return remember(gate, premiumEnabled.isPremiumEnabled, currentOverride) { hasAccess(gate) }
}
