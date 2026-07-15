package com.payslipmax.pdfparser.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FeatureGate {
    PREMIUM_INTELLIGENCE,
    WEALTH_OPTIMIZATION,
    AI_AUDIT,
    TAX_PLANNER,
    DSOP_SIMULATOR,
    ANOMALY_DETECTION,
    CLAIM_GENERATOR,

    /** Gates the offline post-retirement calculators (pension/gratuity/commutation/leave-encashment). */
    RETIREMENT_CALCULATORS,

    /** Gates backup *creation* only. Restore stays free so a new device can always recover its data. */
    BACKUP_RESTORE,
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
 */
class SubscriptionManager(
    private val isPremiumEnabledProvider: () -> Boolean,
    private val isDebugBuildProvider: () -> Boolean = { isDebugBuild() },
) : SubscriptionService {
    private val _devOverride =
        MutableStateFlow(
            if (isDebugBuildProvider()) DevOverride.FORCE_PRO else DevOverride.FOLLOW_FLAG,
        )
    val devOverride: StateFlow<DevOverride> = _devOverride.asStateFlow()

    /** No-op unless this is a debug build — the override cannot be changed in release. */
    fun setDevOverride(override: DevOverride) {
        if (isDebugBuildProvider()) {
            _devOverride.value = override
        }
    }

    override fun hasAccess(feature: FeatureGate): Boolean {
        if (isDebugBuildProvider()) {
            when (_devOverride.value) {
                DevOverride.FORCE_PRO -> return true
                DevOverride.FORCE_FREE -> return false
                DevOverride.FOLLOW_FLAG -> Unit
            }
        }
        return isPremiumEnabledProvider()
    }
}
