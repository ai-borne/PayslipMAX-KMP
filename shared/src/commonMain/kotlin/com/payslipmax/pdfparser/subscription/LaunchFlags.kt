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

    // Flipped false for versionCode 16 (internal testing only) — the paywall build; gates follow the real
    // RevenueCat entitlement. Production/closed testing stay on versionCode 15 (flag true) until promoted.
    const val FREE_LAUNCH_MODE_ANDROID: Boolean = false
}
