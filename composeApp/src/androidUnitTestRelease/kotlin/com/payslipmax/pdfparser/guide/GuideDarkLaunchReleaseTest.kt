package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.AppNavStateSaver
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.screens.guide.isGuideEnabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The release variant until phase E9: no Guide tab, and a saved state naming it (for example from a debug build
 * on the same device) restores to Home rather than to a tab that does not exist.
 */
class GuideDarkLaunchReleaseTest {
    @Test
    fun theGuideIsOffInRelease() {
        assertFalse(isGuideEnabled())
    }

    @Test
    fun aSavedGuideTabRestoresToHomeInRelease() {
        val restored = AppNavStateSaver.restore(listOf(Screen.Guide.name))!!
        assertEquals(Screen.Dashboard, restored.currentTab)
    }
}
