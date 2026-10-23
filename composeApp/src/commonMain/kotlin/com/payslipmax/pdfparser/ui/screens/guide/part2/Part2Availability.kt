package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.guide.model.GuideBundle
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

/**
 * What the Guide shows for the Part II entry: nothing, or the lookup. There is no locked state because lookup is free for everyone
 * (owner, Rounds 5 and 10); Premium starts at drafting, which has its own entry decision.
 */
enum class Part2Entry { HIDDEN, OPEN }

/**
 * The one decision for the entry point: [enabled] is the dark-launch flag (the factory is non-null), and a bundle that ships no Part II
 * section leaves no dead row.
 */
fun part2Entry(
    enabled: Boolean = isPart2Enabled(),
    bundle: GuideBundle,
): Part2Entry = if (enabled && bundle.part2 != null) Part2Entry.OPEN else Part2Entry.HIDDEN

/**
 * What the Guide tab is given to open the lookup: [create] while the feature is on, null while it is off. Null removes the Guide Home
 * row and every Part II level, and [create] (the Koin lookup) is never called, so the flag-off build holds no lookup model.
 */
fun part2FactoryOrNull(
    enabled: Boolean = isPart2Enabled(),
    create: () -> Part2LookupViewModel,
): (() -> Part2LookupViewModel)? = if (enabled) create else null
