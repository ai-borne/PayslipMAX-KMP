package com.payslipmax.pdfparser.nav

import com.payslipmax.pdfparser.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppNavStateTest {
    /** The four bottom-tab roots; every other [Screen] value is a detail-only destination. */
    private val tabRoots = setOf(Screen.Dashboard, Screen.History, Screen.Insights, Screen.Settings)
    private val detailScreens =
        setOf(
            Screen.Representation,
            Screen.TaxPlanning,
            Screen.RetirementPlanning,
            Screen.HelpLegal,
            Screen.FAQ,
            Screen.PrivacyPolicy,
            Screen.PayslipReplica,
        )

    @Test
    fun defaultsToDashboardTabWithNoDetail() {
        val nav = AppNavState()
        assertEquals(Screen.Dashboard, nav.currentTab)
        assertNull(nav.activeDetail)
        assertTrue(nav.canExitApp)
    }

    @Test
    fun pushThenPopReturnsToTabRoot() {
        val nav = AppNavState(currentTab = Screen.Insights)
        nav.push(Screen.TaxPlanning)
        assertEquals(Screen.TaxPlanning, nav.activeDetail)
        assertFalse(nav.canExitApp)

        assertTrue(nav.pop())
        assertNull(nav.activeDetail)
        // The tab root is unchanged by a pop — we return to it, not away from it.
        assertEquals(Screen.Insights, nav.currentTab)
        assertTrue(nav.canExitApp)
    }

    @Test
    fun popAtTabRootIsNoOpAndCanExit() {
        val nav = AppNavState(currentTab = Screen.History)
        assertFalse(nav.pop())
        assertNull(nav.activeDetail)
        assertEquals(Screen.History, nav.currentTab)
        assertTrue(nav.canExitApp)
    }

    @Test
    fun switchTabAlwaysClearsActiveDetail() {
        // Decision 3: leaving a tab discards its pushed detail rather than remembering it.
        // Asserted explicitly across every target tab so the reset behavior can't silently
        // regress into per-tab depth memory.
        for (target in tabRoots) {
            val nav = AppNavState(currentTab = Screen.Dashboard)
            nav.push(Screen.RetirementPlanning)
            assertEquals(Screen.RetirementPlanning, nav.activeDetail)

            nav.switchTab(target)
            assertEquals(target, nav.currentTab)
            assertNull(nav.activeDetail, "switchTab($target) must clear activeDetail")
            assertTrue(nav.canExitApp)
        }
    }

    @Test
    fun switchTabWithoutActiveDetailJustChangesTab() {
        val nav = AppNavState(currentTab = Screen.Dashboard)
        nav.switchTab(Screen.Settings)
        assertEquals(Screen.Settings, nav.currentTab)
        assertNull(nav.activeDetail)
    }

    @Test
    fun screenEnumPartitionsIntoTabRootsAndDetailScreens() {
        // Guards the model's core assumption: exactly the four tab roots are switchTab targets,
        // every other Screen is detail-only. If a new Screen is added, this fails until the caller
        // decides which bucket it belongs to.
        assertEquals(Screen.entries.toSet(), tabRoots + detailScreens)
        assertTrue((tabRoots intersect detailScreens).isEmpty())
    }
}
