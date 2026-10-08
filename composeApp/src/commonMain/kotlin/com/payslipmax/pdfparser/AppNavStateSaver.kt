package com.payslipmax.pdfparser

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.payslipmax.pdfparser.nav.AppNavState
import com.payslipmax.pdfparser.ui.screens.guide.isGuideEnabled

/**
 * Persists [AppNavState] across process death via a [Screen] name list: tab root first, then
 * the pushed detail stack bottom-to-top (decision 9). Restoration is crash-guarded and
 * truncates from the first invalid entry onward — if a saved constant was renamed or removed,
 * or a detail name turns up in the tab slot, everything from that point on is discarded rather
 * than reconstructing a stack ordering the user never actually created.
 */
internal val AppNavStateSaver: Saver<AppNavState, Any> = appNavStateSaver(guideEnabled = isGuideEnabled())

/**
 * [guideEnabled] false (release until E9) never restores the Guide tab, so the app stays at four tabs, and cuts a saved
 * [Screen.GuideCard] (with anything above it). A restored card with no pending target pops itself in its host.
 */
internal fun appNavStateSaver(guideEnabled: Boolean): Saver<AppNavState, Any> =
    listSaver(
        save = { listOf(it.currentTab.name) + it.detailStack.map(Screen::name) },
        restore = { saved ->
            AppNavState(
                currentTab =
                    restoreScreen(saved.getOrNull(0))?.takeIf { it.isTabRoot && (guideEnabled || it != Screen.Guide) }
                        ?: Screen.Dashboard,
                initialDetailStack =
                    saved.drop(1)
                        .map(::restoreScreen)
                        .takeWhile { it != null && !it.isTabRoot && (guideEnabled || it != Screen.GuideCard) }
                        .filterNotNull(),
            )
        },
    )

private fun restoreScreen(name: String?): Screen? =
    name?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
