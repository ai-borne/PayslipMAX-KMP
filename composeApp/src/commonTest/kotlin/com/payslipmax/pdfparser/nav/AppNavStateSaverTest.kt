package com.payslipmax.pdfparser.nav

import androidx.compose.runtime.saveable.SaverScope
import com.payslipmax.pdfparser.AppNavStateSaver
import com.payslipmax.pdfparser.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure-Saver-logic tests for [AppNavStateSaver], split out of `AppBackNavigationTest` (which
 * stayed within its 300-line budget by moving these here) because they exercise `SaverScope`
 * directly and need no Robolectric/Compose UI host — see decision 9 in the back-navigation plan.
 */
class AppNavStateSaverTest {
    // --- Decision 9: state survives process death via the Saver ---

    @Test
    fun saverRoundTripsTabAndDetail() {
        val state = AppNavState(currentTab = Screen.Insights, initialDetailStack = listOf(Screen.TaxPlanning))
        val saved = with(AppNavStateSaver) { SaverScope { true }.save(state) }
        val restored = AppNavStateSaver.restore(saved!!)!!
        assertEquals(Screen.Insights, restored.currentTab)
        assertEquals(Screen.TaxPlanning, restored.activeDetail)
    }

    @Test
    fun saverRoundTripsChainedDetailStack() {
        // Two-level chain (Settings -> PremiumFeatures -> TaxPlanning): a process-death restore must
        // bring back the whole stack, not just the top, so a subsequent pop lands on PremiumFeatures.
        val state =
            AppNavState(
                currentTab = Screen.Settings,
                initialDetailStack = listOf(Screen.PremiumFeatures, Screen.TaxPlanning),
            )
        val saved = with(AppNavStateSaver) { SaverScope { true }.save(state) }
        val restored = AppNavStateSaver.restore(saved!!)!!
        assertEquals(Screen.Settings, restored.currentTab)
        assertEquals(Screen.TaxPlanning, restored.activeDetail)
        assertTrue(restored.pop())
        assertEquals(Screen.PremiumFeatures, restored.activeDetail)
    }

    @Test
    fun saverTruncatesStackFromFirstInvalidEntry() {
        // D2: a corrupt entry mid-stack (e.g. a since-deleted Screen constant) discards it and
        // everything pushed after it, rather than dropping just the bad entry and keeping later
        // ones — that would reconstruct an ordering the user never actually created.
        val restored =
            AppNavStateSaver.restore(
                listOf(Screen.Settings.name, Screen.PremiumFeatures.name, "DeletedScreen", Screen.TaxPlanning.name),
            )!!
        assertEquals(Screen.Settings, restored.currentTab)
        assertEquals(Screen.PremiumFeatures, restored.activeDetail)
        assertTrue(restored.pop())
        assertTrue(restored.canExitApp)
    }

    @Test
    fun saverCrashGuardFallsBackWhenScreenConstantRemoved() {
        // Simulates a user mid-navigation who updates to a build where the persisted Screen
        // constant no longer exists: restoration must fall back, not throw a launch-crash-loop.
        val restored = AppNavStateSaver.restore(listOf("RenamedTabScreen", "DeletedDetailScreen"))!!
        assertEquals(Screen.Dashboard, restored.currentTab)
        assertNull(restored.activeDetail)
        assertTrue(restored.canExitApp)
    }

    @Test
    fun saverRejectsMisbucketedScreenNames() {
        // A detail name in the tab slot (or a tab name in the detail slot) must not silently
        // produce an impossible state — the slot guards coerce each back to a valid bucket.
        val restored = AppNavStateSaver.restore(listOf(Screen.TaxPlanning.name, Screen.Dashboard.name))!!
        assertEquals(Screen.Dashboard, restored.currentTab)
        assertNull(restored.activeDetail)
    }
}
