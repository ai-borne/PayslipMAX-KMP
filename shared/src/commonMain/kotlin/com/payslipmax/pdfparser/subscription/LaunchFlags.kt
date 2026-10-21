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

    // Claim Guide kill switch (docs/Plan/rule_cards/16_guide_phase_plan.md), the only switch: every Guide entry point
    // reads it through isGuideEnabled(), in every build type. false removes the tab, routes and Pay Audit link.
    // Flipped true in phase E9 (2026-10-09), in its own commit.
    const val GUIDE_ENABLED: Boolean = true

    // Claim Guide paywall (owner decision 2026-10-07). false: every user reads the whole Guide; true: only Premium reads
    // key points, authority and details ([FeatureGate.CLAIM_GUIDE]). Flipped only after the owner has cleared the open
    // points on the main rate cards (RP-088 HBA 8.5% and others), because charging for "Unverified point" cards is a
    // trust risk. The gate and the locked UI ship and are tested either way. Flipped true 2026-10-18 by the owner (the only user),
    // waiving five unverified rate cards (see GuideBundleContract) so the locked screens can be seen on a real device.
    const val GUIDE_PAYWALL_ENABLED: Boolean = true

    // TA/DA pre-claim worksheet inside the Guide (docs/Plan/rule_cards/22_ta_da_preclaim_execution_plan.md). false removes the entry
    // point, so the ViewModel and calculators ship but nothing reaches a user. Flipped true in P9 (2026-10-18) after the device and
    // iPhone walks and the owner's review of preclaim.json, in its own commit. It also needs GUIDE_ENABLED: the worksheet lives in
    // the Guide tab. Premium follows GUIDE_PAYWALL_ENABLED.
    const val PRECLAIM_ENABLED: Boolean = true

    // Leave Bank kill switch (docs/Plan/11_LeaveBank_PhasePlan.md), the only switch: every Leave Bank surface (Retirement
    // Calculators card, payslip section, Smart Insight) reads it through isLeaveBankEnabled(). The parser always runs. Flipped true
    // in Phase 6, in its own commit.
    const val LEAVE_BANK_ENABLED: Boolean = false
}
