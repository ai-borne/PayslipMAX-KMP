package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.subscription.LaunchFlags

/**
 * Whether Part II exists at all: the dark-launch flag [LaunchFlags.PART2_ENABLED] and the Guide kill switch [LaunchFlags.GUIDE_ENABLED]
 * (Part II lives in the Guide tab). Switched off, the Guide row, the routes and the Koin module are absent.
 */
fun isPart2Enabled(
    guideEnabled: Boolean = LaunchFlags.GUIDE_ENABLED,
    part2Enabled: Boolean = LaunchFlags.PART2_ENABLED,
): Boolean = guideEnabled && part2Enabled

/** What the Guide shows for the Learn entry: nothing, or the row. Learn is free for everyone, so there is no locked state. */
enum class Part2Entry { HIDDEN, OPEN }

/** The one decision for the entry row: [enabled] is the dark-launch flag, and a bundle that ships no Part II section leaves no dead row. */
fun part2Entry(
    enabled: Boolean = isPart2Enabled(),
    bundle: GuideBundle,
): Part2Entry = if (enabled && bundle.part2 != null) Part2Entry.OPEN else Part2Entry.HIDDEN

/**
 * What the Guide tab is given to open Learn: [create] (the Koin model) while the feature is on, null while it is off. Null removes the
 * Guide Home row and every Part II level, and [create] is never called, so the flag-off build holds no Learn model.
 */
fun <T> part2FactoryOrNull(
    enabled: Boolean = isPart2Enabled(),
    create: () -> T,
): (() -> T)? = if (enabled) create else null
