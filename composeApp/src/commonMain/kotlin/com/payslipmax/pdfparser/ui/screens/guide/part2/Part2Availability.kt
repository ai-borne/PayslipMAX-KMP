package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.subscription.LaunchFlags

/**
 * Whether Part II Orders exists at all: the dark-launch flag [LaunchFlags.PART2_ENABLED] and the Guide kill switch
 * [LaunchFlags.GUIDE_ENABLED] (Part II lives in the Guide tab). Switched off, the tile, the routes and the Koin module are absent and
 * R8 drops the code; the Premium catalog row stays off the paywall too (`isAdvertised`).
 */
fun isPart2Enabled(
    guideEnabled: Boolean = LaunchFlags.GUIDE_ENABLED,
    part2Enabled: Boolean = LaunchFlags.PART2_ENABLED,
): Boolean = guideEnabled && part2Enabled
