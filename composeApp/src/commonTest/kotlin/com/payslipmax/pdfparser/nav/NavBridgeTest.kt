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
    fun nativeStackObservedSyncsStateWithoutRePoppingNative() {
        // A completed edge-swipe: state must clear, but we must NOT ask the native stack to pop
        // again (it already did) — otherwise the delegate/bridge form a feedback loop.
        val navState = AppNavState(currentTab = Screen.Settings)
        val (bridge, rec) = bridge(navState)
        bridge.navigateToDetail(Screen.TaxPlanning)

        bridge.onNativeStackObserved(detailCount = 0)

        assertNull(navState.activeDetail)
        assertEquals(0, rec.popCount, "native pop must not be re-triggered from the sync callback")
    }

    @Test
    fun nestedEdgeSwipeLandsTheStateOnTheScreenNativeShows() {
        // Item 5: History → payslip → Retirement Calculators, then an edge-swipe. Native now shows the payslip (one detail); before the
        // fix only a return to the root was reported, so the state stayed on Retirement Calculators.
        val navState = AppNavState(currentTab = Screen.History)
        val (bridge, rec) = bridge(navState)
        bridge.navigateToDetail(Screen.PayslipReplica)
        bridge.navigateToDetail(Screen.RetirementCalculators)

        bridge.onNativeStackObserved(detailCount = 1)

        assertEquals(listOf(Screen.PayslipReplica), navState.detailStack)
        assertEquals(0, rec.popCount)
    }

    @Test
    fun pushShownByNativeDoesNotChangeTheState() {
        // didShow also fires after every push; at the pushed depth it must be a no-op, never a pop or a re-push.
        val navState = AppNavState(currentTab = Screen.Insights)
        val (bridge, rec) = bridge(navState)
        bridge.navigateToDetail(Screen.PayAudit)
        bridge.navigateToDetail(Screen.GuideCard)

        bridge.onNativeStackObserved(detailCount = 2)

        assertEquals(listOf(Screen.PayAudit, Screen.GuideCard), navState.detailStack)
        assertEquals(listOf(Screen.PayAudit, Screen.GuideCard), rec.pushed)
        assertEquals(0, rec.popCount)
    }

    @Test
    fun navigateToDetailPushesPayslipReplica() {
        // Documents that the bridge is generic over Screen.PayslipReplica (Phase 4's iOS cutover)
        // exactly like every other detail screen — no special-casing needed here.
        val navState = AppNavState(currentTab = Screen.History)
        val (bridge, rec) = bridge(navState)

        bridge.navigateToDetail(Screen.PayslipReplica)

        assertEquals(Screen.PayslipReplica, navState.activeDetail)
        assertEquals(listOf(Screen.PayslipReplica), rec.pushed)
    }

    @Test
    fun redundantNativeSyncAfterRequestPopIsHarmless() {
        // requestPop pops native; the delegate's didShow then also reports the new depth.
        // The state is already at that depth, so the sync is a safe no-op (no double pop).
        val navState = AppNavState(currentTab = Screen.Settings)
        val (bridge, rec) = bridge(navState)
        bridge.navigateToDetail(Screen.PremiumFeatures)
        bridge.navigateToDetail(Screen.FAQ)

        bridge.requestPop()
        bridge.onNativeStackObserved(detailCount = 1)

        assertEquals(Screen.PremiumFeatures, navState.activeDetail)
        assertEquals(1, rec.popCount, "only the user-initiated back pops native; the sync is a no-op")
    }

    @Test
    fun guideCardFromPayAuditPushesNativeAndBackReturnsToTheFinding() {
        // E7, iOS path: the card is a native view controller above Pay Audit; the in-screen Back pops it once.
        val navState = AppNavState(currentTab = Screen.Insights)
        val (bridge, rec) = bridge(navState)
        bridge.navigateToDetail(Screen.PayAudit)
        bridge.navigateToDetail(Screen.GuideCard)

        assertEquals(listOf(Screen.PayAudit, Screen.GuideCard), rec.pushed)
        bridge.requestPop()

        assertEquals(Screen.PayAudit, navState.activeDetail)
        assertEquals(1, rec.popCount)
    }

    @Test
    fun guideCardIsNotPushedWhileLocked() {
        val navState = AppNavState(currentTab = Screen.Insights, initialDetailStack = listOf(Screen.PayAudit))
        val (bridge, rec) = bridge(navState, locked = true)

        bridge.navigateToDetail(Screen.GuideCard)

        assertEquals(Screen.PayAudit, navState.activeDetail)
        assertTrue(rec.pushed.isEmpty(), "a locked app never shows a card")
    }
}
