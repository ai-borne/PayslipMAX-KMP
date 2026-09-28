package com.payslipmax.pdfparser.subscription

import com.payslipmax.pdfparser.billing.BillingManager
import com.payslipmax.pdfparser.billing.SubscriptionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FeatureGate {
    PREMIUM_INTELLIGENCE,
    WEALTH_OPTIMIZATION,
    TAX_PLANNER,
    DSOP_SIMULATOR,
    ANOMALY_DETECTION,
    CLAIM_GENERATOR,

    /** Gates the offline post-retirement calculators (pension/gratuity/commutation/leave-encashment). */
    RETIREMENT_CALCULATORS,

    /** Gates backup *creation* only. Restore stays free so a new device can always recover its data. */
    BACKUP_RESTORE,

    /** Gates the PCDA(O) Military Financial Intelligence Engine & Situational Matrix. */
    PAYSLIPMAX_AI,
}

/**
 * Debug-only, 3-state entitlement override for exercising both locked and unlocked UX in dev builds.
 *
 * - [FOLLOW_FLAG]: defer to the real `isPremiumEnabled` flag (the production behaviour).
 * - [FORCE_PRO]: grant every gate regardless of the flag (default in debug, preserves prior dev behaviour).
 * - [FORCE_FREE]: deny every gate regardless of the flag (to test locked/teaser UX).
 *
 * The override is in-memory only — it resets to its default on every launch, which is the safe
 * default for a bypass. It is inert in release builds (see [SubscriptionManager]).
 */
enum class DevOverride { FOLLOW_FLAG, FORCE_PRO, FORCE_FREE }

interface SubscriptionService {
    fun hasAccess(feature: FeatureGate): Boolean
}

/**
 * Single source of truth for PRO entitlement. Every gated surface asks [hasAccess]; nothing reads
 * the raw premium flag for gating decisions.
 *
 * [isDebugBuildProvider] is injected (rather than calling the global [isDebugBuild]) so the debug
 * guard is unit-testable without a platform build. In release it resolves to `false`, which makes
 * the [DevOverride] mechanism completely inert: [hasAccess] then always follows the real flag.
 *
 * [isTestFlightBuildProvider] (backed by [isTestFlightBuild]) extends override eligibility to
 * TestFlight/sandbox installs only — never real App Store production — so Phase 7 of
 * `docs/Launch/08_ios_monetization_phaseplan.md` can flip `FORCE_FREE` in a TestFlight build to
 * make the real paywall reachable for a sandbox purchase, without ever touching
 * [LaunchFlags.FREE_LAUNCH_MODE_IOS] itself.
 *
 * [isFreeLaunchModeProvider] (backed by [isFreeLaunchModePlatform]) sits below the override but
 * above billing/the flag: it's the v1.0 "ship unlocked" launch strategy (see
 * `docs/Launch/05_launch_strategy_and_resolution.md`), so QA can still use `FORCE_FREE` in an
 * eligible build to exercise locked UX, but real users get every gate open regardless of billing
 * state.
 */
class SubscriptionManager(
    private val isPremiumEnabledProvider: () -> Boolean,
    private val isDebugBuildProvider: () -> Boolean = { isDebugBuild() },
    private val billingManager: BillingManager? = null,
    private val isFreeLaunchModeProvider: () -> Boolean = { isFreeLaunchModePlatform() },
    private val isTestFlightBuildProvider: () -> Boolean = { isTestFlightBuild() },
) : SubscriptionService {
    private val isOverrideEligibleProvider: () -> Boolean = { isDebugBuildProvider() || isTestFlightBuildProvider() }

    private val _devOverride =
        MutableStateFlow(
            if (isDebugBuildProvider()) DevOverride.FORCE_PRO else DevOverride.FOLLOW_FLAG,
        )
    val devOverride: StateFlow<DevOverride> = _devOverride.asStateFlow()

    /** No-op unless this is a debug build or TestFlight/sandbox install — inert in App Store production. */
    fun setDevOverride(override: DevOverride) {
        if (isOverrideEligibleProvider()) {
            _devOverride.value = override
        }
    }

    override fun hasAccess(feature: FeatureGate): Boolean {
        if (isOverrideEligibleProvider()) {
            when (_devOverride.value) {
                DevOverride.FORCE_PRO -> return true
                DevOverride.FORCE_FREE -> return false
                DevOverride.FOLLOW_FLAG -> Unit
            }
        }
        if (isFreeLaunchModeProvider()) return true
        return when (val state = billingManager?.subscriptionState?.value) {
            is SubscriptionState.Active -> true
            is SubscriptionState.Inactive -> false
            is SubscriptionState.Unknown, null -> isPremiumEnabledProvider()
        }
    }
}
