package com.payslipmax.pdfparser.nav

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.payslipmax.pdfparser.Screen

/**
 * Flat, single-level back-stack model for the app's navigation.
 *
 * The app is exactly one level deep: a current tab root ([currentTab], one of the four
 * [Screen] bottom-tab destinations) with at most one pushed detail screen ([activeDetail]).
 * Because switching tabs resets the pushed detail (decision 3 in the back-navigation plan),
 * there is no need to remember per-tab depth, so a single flat level suffices.
 *
 * Both mutable fields are backed by [mutableStateOf] so Compose observes changes and
 * recomposes — a plain mutable class would mutate silently and the UI would never update.
 */
@Stable
class AppNavState(
    currentTab: Screen = Screen.Dashboard,
    activeDetail: Screen? = null,
) {
    var currentTab by mutableStateOf(currentTab)
        private set

    var activeDetail by mutableStateOf(activeDetail)
        private set

    /** Push a detail screen on top of the current tab root. */
    fun push(screen: Screen) {
        activeDetail = screen
    }

    /**
     * Pop the active detail screen, returning to the current tab root.
     * @return true if a detail was popped, false if already at a tab root (no-op).
     */
    fun pop(): Boolean {
        if (activeDetail == null) return false
        activeDetail = null
        return true
    }

    /**
     * Switch to a different bottom tab. Always clears [activeDetail] — leaving a tab
     * discards its pushed detail rather than remembering it (decision 3: tabs reset to
     * root on switch-away, so a pushed detail is never restored when returning to a tab).
     */
    fun switchTab(tab: Screen) {
        currentTab = tab
        activeDetail = null
    }

    /** True iff there is nothing to pop, i.e. system back should exit the app. */
    val canExitApp: Boolean
        get() = activeDetail == null
}
