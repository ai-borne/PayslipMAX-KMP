package com.payslipmax.pdfparser.nav

import com.payslipmax.pdfparser.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: a reminder tap lands on the Guide tab, but on iOS a pushed detail is a native view controller that covers the whole tab tree. The
 * bridge must close every detail on the native stack in one step and keep the shared state in step with it, or the officer would see the
 * old detail instead of Due and its back button would no longer match the state. While locked it does nothing, like every other push.
 */
class NavBridgePopToRootTest {
    private var popsToRoot = 0
    private var pops = 0

    private fun bridge(
        navState: AppNavState,
        locked: Boolean = false,
    ) = NavBridge(navState, isLocked = { locked }, popFromNative = { pops++ }, popToRootNative = { popsToRoot++ })

    @Test
    fun itClosesEveryDetailOnTheNativeStackInOneStepAndEmptiesTheState() {
        val navState = AppNavState(currentTab = Screen.Settings)
        navState.push(Screen.PremiumFeatures)
        navState.push(Screen.TaxPlanning)

        bridge(navState).popToRoot()

        assertEquals(1, popsToRoot, "one native call, not one per detail")
        assertEquals(0, pops, "it is not a sequence of single pops")
        assertNull(navState.activeDetail)
        assertEquals(Screen.Settings, navState.currentTab, "the tab is not changed here")
    }

    @Test
    fun withNoDetailOpenTheNativeStackIsNotTouched() {
        bridge(AppNavState(currentTab = Screen.Settings)).popToRoot()

        assertEquals(0, popsToRoot)
    }

    @Test
    fun whileLockedNothingHappens() {
        val navState = AppNavState(currentTab = Screen.Settings)
        navState.push(Screen.PremiumFeatures)

        bridge(navState, locked = true).popToRoot()

        assertEquals(0, popsToRoot)
        assertEquals(Screen.PremiumFeatures, navState.activeDetail)
    }
}
