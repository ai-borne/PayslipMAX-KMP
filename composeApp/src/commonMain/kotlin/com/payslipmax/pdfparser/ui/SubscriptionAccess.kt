package com.payslipmax.pdfparser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import kotlinx.coroutines.flow.StateFlow

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
