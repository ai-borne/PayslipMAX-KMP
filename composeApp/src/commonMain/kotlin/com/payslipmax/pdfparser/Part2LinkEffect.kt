package com.payslipmax.pdfparser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.payslipmax.pdfparser.nav.AppNavState
import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import org.koin.compose.koinInject

/**
 * Opens the tap a reminder notification left behind: takes it (checked against the database, cleared either way) and shows Guide, Part II,
 * Due. True when something was opened. Without the Guide the tap is dropped, since a build without it has nowhere to send the officer.
 */
internal suspend fun PendingPart2Link.openIn(
    guideNavState: GuideNavState?,
    onNavigate: (Screen) -> Unit,
): Boolean {
    take() ?: return false
    guideNavState ?: return false
    openDue(guideNavState, onNavigate)
    return true
}

/** Opens a waiting reminder tap as soon as this is composed, which is only ever in the unlocked app: while locked the tap stays pending. */
@Composable
internal fun Part2LinkOpener(
    pending: PendingPart2Link,
    guideNavState: GuideNavState?,
    onNavigate: (Screen) -> Unit,
) {
    // One collector for as long as this is composed. A LaunchedEffect keyed on the link would be cancelled the moment take() clears it, which is
    // before its database lookup finishes: the tap would never open (found on the Pixel 9, 2026-10-26).
    LaunchedEffect(pending) { pending.link.collect { pending.openIn(guideNavState, onNavigate) } }
}

/** The app's one call: nothing is composed, and Koin is never asked, while Part II is off. A tab switch also closes any open detail. */
@Composable
internal fun Part2LinkEffect(
    navState: AppNavState,
    guideNavState: GuideNavState?,
    enabled: Boolean = isPart2Enabled(),
) {
    if (!enabled) return
    Part2LinkOpener(koinInject(), guideNavState) { navState.switchTab(it) }
}
