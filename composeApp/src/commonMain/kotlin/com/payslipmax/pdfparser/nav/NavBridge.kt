package com.payslipmax.pdfparser.nav

import com.payslipmax.pdfparser.Screen

/**
 * Bidirectional bridge between the shared [AppNavState] and a platform's native navigation
 * stack (iOS `UINavigationController`). It keeps [AppNavState] as the single source of truth
 * while delegating the actual push/pop of native view controllers to the [pushToNative]/
 * [popFromNative] closures supplied by the platform layer.
 *
 * This type is pure Kotlin (no UIKit) so it can be unit-tested in `commonTest`; the iOS
 * factories in `MainViewController.kt` wire the closures to real `UINavigationController` calls.
 *
 * @param navState the shared, Compose-observable navigation model to mutate.
 * @param isLocked reports whether the app is currently lock-screened; a `true` result blocks
 *   [navigateToDetail] entirely (decision 5, iOS half — never push content while locked).
 * @param pushToNative asks the platform to push a native VC hosting [Screen].
 * @param popFromNative asks the platform to pop the top native VC.
 */
class NavBridge(
    private val navState: AppNavState,
    private val isLocked: () -> Boolean = { false },
    private val pushToNative: (Screen) -> Unit = {},
    private val popFromNative: () -> Unit = {},
) {
    /**
     * Navigate to a detail [screen]: record it in [navState] and push its native VC.
     * Suppressed while the app is locked so a background gesture can never reveal content.
     */
    fun navigateToDetail(screen: Screen) {
        if (isLocked()) return
        navState.push(screen)
        pushToNative(screen)
    }

    /**
     * A user-initiated back (in-screen back header) — pop [navState] and, only if something
     * was actually popped, pop the matching native VC.
     */
    fun requestPop() {
        if (navState.pop()) popFromNative()
    }

    /**
     * A completed native edge-swipe (or any native pop) — sync [navState] *without* asking
     * the platform to pop again, breaking the feedback loop the native delegate would
     * otherwise create. [AppNavState.pop] is idempotent, so a redundant call after
     * [requestPop] already cleared the detail is a harmless no-op.
     */
    fun onNativePopObserved() {
        navState.pop()
    }
}
