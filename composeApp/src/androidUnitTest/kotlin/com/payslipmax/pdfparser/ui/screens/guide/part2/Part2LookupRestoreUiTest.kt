package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: Android can kill the process on a code page and iOS can resume into a build with the flag off, so a restored Part II stack must
 * open on the same page when it is still offered, and must be cut back to Guide Home, never shown half-broken, when it is not. The tab
 * re-tap goes home from any depth.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2LookupRestoreUiTest : Part2UiBase() {
    private val codePageStack = listOf(Part2Destination.Home, Part2Destination.Ribbon("leave"), Part2Destination.Case("leave_one"), Part2Destination.Code("FIRSTCODE"))

    @Test
    fun aRestoredCodePageOpensOnTheSamePageAndBackWalksUp() {
        val nav = show(GuideNavState(codePageStack))
        assertEquals(Part2Destination.Code("FIRSTCODE"), nav.current)
        see(Part2Strings.chapterAndPage(1, 28)).assertIsDisplayed()
        back()
        assertEquals(Part2Destination.Case("leave_one"), nav.current)
    }

    @Test
    fun aRestoredStackIsCutBackToGuideHomeWhenTheFlagIsNowOff() {
        val nav = show(GuideNavState(codePageStack), part2 = null)
        assertEquals(GuideDestination.Home, nav.current)
        composeRule.onAllNodesWithText(Part2Strings.entryTitle).assertCountEquals(0)
        assertEquals(0, models.size, "a flag-off build never asks Koin for the model")
    }

    @Test
    fun aRestoredRibbonThatIsNotBuiltIsCutAtThatLevel() {
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Ribbon("career"))))
        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun aRestoredCodeTheBundleLacksIsCutAtThatLevel() {
        val nav = show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Code("NOSUCHCODE"))))
        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun theTabReTapGoesToGuideHomeFromACodePage() {
        val nav = show(GuideNavState(codePageStack))
        nav.popToHome()
        settle()
        assertEquals(GuideDestination.Home, nav.current)
        see(Part2Strings.entryTitle).assertIsDisplayed()
    }

    @Test
    fun anOldBundleHidesTheRowAndCutsARestoredStack() {
        oldBundle = true
        val nav = show(GuideNavState(codePageStack))
        assertEquals(GuideDestination.Home, nav.current)
        composeRule.onAllNodesWithText(Part2Strings.entryTitle).assertCountEquals(0)
        assertEquals(0, models.size, "no section, no model, even with the flag on")
    }

    private var oldBundle = false

    override fun bundle(): GuideBundle = if (oldBundle) SyntheticGuideBundle.withPart2().copy(part2 = null) else super.bundle()
}
