package com.payslipmax.pdfparser.nav

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.payslipmax.pdfparser.Screen

/**
 * Multi-level back-stack model for the app's navigation.
 *
 * A current tab root ([currentTab], one of the four [Screen] bottom-tab destinations) owns an
 * unbounded stack of pushed detail screens ([detailStack], top = [activeDetail]). A single detail
 * can itself open a further detail — e.g. [Screen.PremiumFeatures] chaining into
 * [Screen.TaxPlanning] — so a flat single-slot model is not sufficient: it would silently drop
 * intermediate screens on back-navigation. Because switching tabs resets the whole pushed stack
 * (decision 3 in the back-navigation plan), there is no need to remember per-tab depth.
 *
 * Both mutable fields are backed by [mutableStateOf] so Compose observes changes and
 * recomposes — a plain mutable class would mutate silently and the UI would never update.
 */
@Stable
class AppNavState(
    currentTab: Screen = Screen.Dashboard,
    initialDetailStack: List<Screen> = emptyList(),
) {
    var currentTab by mutableStateOf(currentTab)
        private set

    var detailStack: List<Screen> by mutableStateOf(initialDetailStack)
        private set

    /** The top of [detailStack], or null if the current tab is showing its root. */
    val activeDetail: Screen?
        get() = detailStack.lastOrNull()

    /** Push a detail screen on top of the current stack. */
    fun push(screen: Screen) {
        detailStack = detailStack + screen
    }

    /**
     * Pop the top detail screen, returning to whatever is beneath it (another detail, or the
     * current tab root if the stack is now empty).
     * @return true if a detail was popped, false if already at a tab root (no-op).
     */
    fun pop(): Boolean {
        if (detailStack.isEmpty()) return false
        detailStack = detailStack.dropLast(1)
        return true
    }

    /**
     * Switch to a different bottom tab. Always clears the entire [detailStack] — leaving a tab
     * discards its pushed details rather than remembering them (decision 3: tabs reset to
     * root on switch-away, so a pushed detail chain is never restored when returning to a tab).
     */
    fun switchTab(tab: Screen) {
        currentTab = tab
        detailStack = emptyList()
    }

    /** True iff there is nothing to pop, i.e. system back should exit the app. */
    val canExitApp: Boolean
        get() = detailStack.isEmpty()
}
