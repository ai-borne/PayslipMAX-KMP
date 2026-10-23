package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2LookupViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimViewModel

/**
 * The Guide tab's optional, dark-launched features, each with the host it draws from. A new feature adds a field here and a line to
 * [rememberGuideHosts], so `GuideTab.kt` does not grow with every feature (plan 12, Phase 11 item 13).
 */
internal class GuideHosts(
    val preClaim: PreClaimHost,
    val part2: Part2Host,
)

/** Builds every host once per composition. A null factory is that feature's flag being off, and its model is then never made. */
@Composable
internal fun rememberGuideHosts(
    preClaim: (() -> PreClaimViewModel)?,
    part2: (() -> Part2LookupViewModel)?,
    access: GuideAccess,
    navState: GuideNavState,
    bundle: GuideBundle,
): GuideHosts = GuideHosts(rememberPreClaimHost(preClaim, access, navState), rememberPart2Host(part2, bundle))
