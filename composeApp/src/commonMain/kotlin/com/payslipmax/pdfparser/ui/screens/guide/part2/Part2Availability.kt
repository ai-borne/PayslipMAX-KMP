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
 * What the Guide tab is given to open Part II: [create] (the Koin `Part2Models`) while the feature is on, null while it is off. Null removes
 * the Guide Home row and every Part II level, and [create] is never called, so the flag-off build holds no Part II model.
 */
fun <T> part2FactoryOrNull(
    enabled: Boolean = isPart2Enabled(),
    create: () -> T,
): (() -> T)? = if (enabled) create else null

/**
 * What drafting shows. [OPEN] for an entitled user; [VIEW_ONLY] for one whose Premium has ended but who still holds drafts (they can open
 * and delete them, not create or change one); [LOCKED] for a user with neither, who sees the upgrade panel.
 */
enum class Part2DraftEntry { LOCKED, VIEW_ONLY, OPEN }

fun part2DraftEntry(
    unlocked: Boolean,
    hasDrafts: Boolean,
): Part2DraftEntry =
    when {
        unlocked -> Part2DraftEntry.OPEN
        hasDrafts -> Part2DraftEntry.VIEW_ONLY
        else -> Part2DraftEntry.LOCKED
    }
