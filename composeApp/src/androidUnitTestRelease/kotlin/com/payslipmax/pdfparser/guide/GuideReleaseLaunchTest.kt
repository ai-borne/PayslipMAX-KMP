package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.AppNavStateSaver
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.screens.guide.isGuideEnabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The release variant from phase E9: the Guide tab exists, so a saved state naming it restores to the Guide tab (it
 * restored to Home while the Guide was dark).
 */
class GuideReleaseLaunchTest {
    @Test
    fun theGuideIsOnInRelease() {
        assertTrue(isGuideEnabled())
    }

    @Test
    fun aSavedGuideTabRestoresToTheGuideInRelease() {
        val restored = AppNavStateSaver.restore(listOf(Screen.Guide.name))!!
        assertEquals(Screen.Guide, restored.currentTab)
    }
}
