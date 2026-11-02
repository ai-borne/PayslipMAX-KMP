package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: noting a casualty is the point of the Orders Bank, and the officer arrives at a casualty page to do exactly that. On a phone the
 * page is long (what the order needs, the plain-English page, what is published with it), so an Add at the end of the list made him scroll
 * past everything first. It stays on screen, whatever has been scrolled, and there is only ever one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class Part2AddPinnedUiTest : Part2UiBase() {
    @Test
    fun theAddButtonIsOnScreenWithoutScrollingOnAPhoneSizedScreen() {
        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))))

        composeRule.onNodeWithText(LearnStrings.addToBank).assertIsDisplayed().assertIsEnabled()
        assertEquals(1, composeRule.onAllNodesWithText(LearnStrings.addToBank).fetchSemanticsNodes().size, "one Add, not a second at the end of the list")
    }
}
