package com.payslipmax.pdfparser.nav

import com.payslipmax.pdfparser.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Bridge-contract tests for [NavBridge] using fake Swift-side closures (same style the existing
 * screen tests fake `onPickPdf`/`onOpenPdf`). Proves the Kotlin half of the iOS native-nav bridge
 * mutates [AppNavState] correctly and only drives the native stack when it should.
 */
class NavBridgeTest {
    private class Recorder {
        val pushed = mutableListOf<Screen>()
        var popCount = 0
    }

    private fun bridge(
        navState: AppNavState = AppNavState(currentTab = Screen.Settings),
        locked: Boolean = false,
        recorder: Recorder = Recorder(),
    ): Pair<NavBridge, Recorder> =
        NavBridge(
            navState = navState,
            isLocked = { locked },
            pushToNative = { recorder.pushed.add(it) },
            popFromNative = { recorder.popCount++ },
        ) to recorder

    @Test
    fun navigateToDetailPushesStateAndNativeStack() {
        val navState = AppNavState(currentTab = Screen.Settings)
        val (bridge, rec) = bridge(navState)

        bridge.navigateToDetail(Screen.TaxPlanning)

        assertEquals(Screen.TaxPlanning, navState.activeDetail)
        assertEquals(listOf(Screen.TaxPlanning), rec.pushed, "native push must fire exactly once")
    }

    @Test
    fun navigateToDetailIsSuppressedWhileLocked() {
        // Decision 5 (iOS half): a locked app never pushes content — neither state nor native.
        val navState = AppNavState(currentTab = Screen.Settings)
        val (bridge, rec) = bridge(navState, locked = true)

        bridge.navigateToDetail(Screen.RetirementPlanning)

        assertNull(navState.activeDetail)
        assertTrue(rec.pushed.isEmpty(), "no native push while locked")
    }

    @Test
    fun requestPopClearsStateAndPopsNativeOnce() {
        val navState = AppNavState(currentTab = Screen.Insights)
        val (bridge, rec) = bridge(navState)
        bridge.navigateToDetail(Screen.Representation)

        bridge.requestPop()

        assertNull(navState.activeDetail)
        assertEquals(1, rec.popCount, "in-screen back pops the native stack exactly once")
    }

    @Test
    fun requestPopAtRootDoesNotTouchNativeStack() {
        // Nothing pushed → nothing to pop; must not send a spurious native pop.
        val navState = AppNavState(currentTab = Screen.History)
        val (bridge, rec) = bridge(navState)

        bridge.requestPop()

        assertEquals(0, rec.popCount)
    }

    @Test
    fun onNativePopObservedSyncsStateWithoutRePoppingNative() {
        // A completed edge-swipe: state must clear, but we must NOT ask the native stack to pop
        // again (it already did) — otherwise the delegate/bridge form a feedback loop.
        val navState = AppNavState(currentTab = Screen.Settings)
        val (bridge, rec) = bridge(navState)
        bridge.navigateToDetail(Screen.TaxPlanning)

        bridge.onNativePopObserved()

        assertNull(navState.activeDetail)
        assertEquals(0, rec.popCount, "native pop must not be re-triggered from the sync callback")
    }

    @Test
    fun redundantNativeSyncAfterRequestPopIsHarmless() {
        // requestPop pops native; the delegate's didShow then also calls onNativePopObserved.
        // pop() is idempotent, so the second call is a safe no-op (no double state change).
        val navState = AppNavState(currentTab = Screen.Settings)
        val (bridge, rec) = bridge(navState)
        bridge.navigateToDetail(Screen.HelpLegal)

        bridge.requestPop()
        bridge.onNativePopObserved()

        assertNull(navState.activeDetail)
        assertEquals(1, rec.popCount, "only the user-initiated back pops native; the sync is a no-op")
    }
}
