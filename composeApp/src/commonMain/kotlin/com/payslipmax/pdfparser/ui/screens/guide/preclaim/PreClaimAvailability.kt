package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.subscription.LaunchFlags
import com.payslipmax.pdfparser.ui.screens.guide.GuideAccess

/**
 * Whether the TA/DA pre-claim worksheet exists at all: the dark-launch flag [LaunchFlags.PRECLAIM_ENABLED] and the Guide kill switch
 * [LaunchFlags.GUIDE_ENABLED] (the worksheet lives in the Guide tab). Switched off, the entry point, the Koin module and so the
 * ViewModel are absent and R8 drops the code.
 */
fun isPreClaimEnabled(
    guideEnabled: Boolean = LaunchFlags.GUIDE_ENABLED,
    preClaimEnabled: Boolean = LaunchFlags.PRECLAIM_ENABLED,
): Boolean = guideEnabled && preClaimEnabled

/** What the Guide shows for the pre-claim entry: nothing, an upgrade prompt, or the worksheet. */
enum class PreClaimEntry { HIDDEN, LOCKED, OPEN }

/**
 * The one decision for the entry point. The worksheet is a Premium feature (owner, 2026-10-11) and follows the Guide's own
 * entitlement ([GuideAccess], so a QA override shows the locked panel exactly as it does for a card), never a second paywall.
 */
fun preClaimEntry(
    access: GuideAccess,
    enabled: Boolean = isPreClaimEnabled(),
): PreClaimEntry =
    when {
        !enabled -> PreClaimEntry.HIDDEN
        access.isUnlocked -> PreClaimEntry.OPEN
        else -> PreClaimEntry.LOCKED
    }

/**
 * What the Guide tab is given to open worksheets: [create] while the feature is on, null while it is off. Null removes the Home row,
 * the card button and the destination, and [create] (the Koin lookup) is never called, so the flag-off build holds no worksheet.
 */
fun preClaimFactoryOrNull(
    enabled: Boolean = isPreClaimEnabled(),
    create: () -> PreClaimViewModel,
): (() -> PreClaimViewModel)? = if (enabled) create else null
