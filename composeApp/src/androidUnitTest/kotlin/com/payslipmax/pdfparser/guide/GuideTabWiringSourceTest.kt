package com.payslipmax.pdfparser.guide

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural guard on `GuideTab.kt` (Part II plan, Phase 11 item 13). Each dark-launched feature (the TA/DA worksheet, Part II) used to add a
 * host, a parameter and a branch to this one file, which sits near the 300-line hard limit. The hosts now live in `GuideHosts`, so a new
 * feature adds a field there and its own file, and this file must not grow past where the fold left it. Raising [MAX_LINES] needs the owner.
 */
class GuideTabWiringSourceTest {
    private val tab = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide/GuideTab.kt")

    @Test
    fun theTabDoesNotGrowWithEachFeature() {
        assertTrue(tab.isFile, "run from the composeApp module: ${tab.absolutePath}")
        val lines = tab.readLines().size
        assertTrue(lines <= MAX_LINES, "GuideTab.kt is $lines lines, the limit after the fold is $MAX_LINES: put new wiring in GuideHosts or the feature's own file")
    }

    @Test
    fun theTabNamesTheHolderAndNoFeatureHost() {
        val text = tab.readText()
        assertTrue("rememberGuideHosts(" in text, "the tab builds its hosts through the one holder")
        assertEquals(emptyList(), listOf("PreClaimHost", "Part2Host", "rememberPreClaimHost", "rememberPart2Host").filter { it in text }, "a host type or builder named here is wiring that belongs in GuideHosts")
    }

    private companion object {
        const val MAX_LINES = 263
    }
}
