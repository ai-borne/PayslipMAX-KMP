package com.payslipmax.pdfparser.subscription

/**
 * Single source of truth for the v1.0 "free launch" strategy (see
 * `docs/Launch/05_launch_strategy_and_resolution.md`), split per-platform per
 * `docs/Launch/08_ios_monetization_phaseplan.md` Phase 1 so iOS and Android can flip
 * independently — Android is still blocked by Google Play's mandatory 14-day closed-testing
 * window before BillDesk merchant KYC can be completed, while iOS's RevenueCat/StoreKit
 * rollout proceeds separately. Call sites read the platform-specific constant via
 * [isFreeLaunchModePlatform] rather than either constant directly.
 */
object LaunchFlags {
    // Flipped false in Phase 8 (docs/Launch/08_ios_monetization_phaseplan.md) — iOS monetization
    // is live from v1.2: gates now follow the real RevenueCat entitlement.
    const val FREE_LAUNCH_MODE_IOS: Boolean = false

    // true again from versionCode 18 (2026-10-03): BillDesk merchant KYC is still pending (about 20-30 days), so
    // Google Play sales may be paused or fail and a paywall could not be passed. Android stays free until KYC
    // clears and a real-money purchase succeeds; then flip to false (docs/Launch/07, section 4). versionCodes
    // 16 and 17 were the paywall builds (internal testing only).
    const val FREE_LAUNCH_MODE_ANDROID: Boolean = true

    // Claim Guide dark launch (docs/Plan/rule_cards/16_guide_phase_plan.md). Every Guide entry point reads it;
    // false keeps release builds exactly as before the Guide. Flipped true only in phase E9, in its own commit.
    const val GUIDE_ENABLED: Boolean = false

    // Claim Guide paywall (owner decision 2026-10-07). false: every user reads the whole Guide; true: only Premium reads
    // key points, authority and details ([FeatureGate.CLAIM_GUIDE]). Flipped only after the owner has cleared the open
    // points on the main rate cards (RP-088 HBA 8.5% and others), because charging for "Unverified point" cards is a
    // trust risk. The gate and the locked UI ship and are tested either way.
    const val GUIDE_PAYWALL_ENABLED: Boolean = false
}
