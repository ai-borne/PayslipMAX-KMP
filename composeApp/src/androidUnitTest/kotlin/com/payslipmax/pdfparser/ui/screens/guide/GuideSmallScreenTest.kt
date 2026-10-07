package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.ui.components.AppBottomBar
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Labels on a small phone (360dp wide, the smallest common Android width; iPhone SE is 375pt): the real area and
 * case titles and the five tab labels must lay out without being cut off. Titles are bundle content, so a longer
 * title added later is caught here. At 320dp the existing "Dashboard" tab label wraps to two lines once there are
 * five tabs; that is recorded in the plan's EP list for the E9 release decision.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
// Real text shaping: the default (legacy) Robolectric graphics measure about 1px per character, so overflow is meaningless there.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuideSmallScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }

    /**
     * Not `hasVisualOverflow`: a wrapping Text reports width "overflow" whenever it is narrower than its slot.
     * Clipping that a user would see is an ellipsis or text cut at the bottom; [singleLine] also rejects a wrap.
     */
    private fun SemanticsNodeInteraction.assertNotClipped(
        label: String,
        singleLine: Boolean = false,
    ) {
        val layouts = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        val ellipsized = (0 until layout.lineCount).any(layout::isLineEllipsized)
        assertFalse(ellipsized || layout.didOverflowHeight, "'$label' is cut off at 360dp")
        if (singleLine) assertEquals(1, layout.lineCount, "'$label' wraps at 360dp")
    }

    @Test
    fun everyRealAreaTitleFitsOnASmallPhone() {
        val areas = bundle.toReady().areas
        composeRule.setContent { GuideHomeScreen(areas, onOpenArea = {}) }

        areas.forEach { tile ->
            composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(tile.title))
            composeRule.onNodeWithText(tile.title).assertNotClipped(tile.title)
        }
    }

    @Test
    fun everyRealCaseTitleFitsOnASmallPhone() {
        var area by mutableStateOf(bundle.nav.first().toContent())
        composeRule.setContent { GuideAreaScreen(area, onBack = {}, onOpenCase = {}) }
        bundle.nav.forEach { next ->
            area = next.toContent()
            composeRule.waitForIdle()
            area.cases.forEach { tile ->
                composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(tile.title))
                composeRule.onNodeWithText(tile.title).assertNotClipped(tile.title)
            }
        }
    }

    @Test
    fun fiveTabLabelsFitOnASmallPhone() {
        composeRule.setContent { AppBottomBar(currentScreen = Screen.Guide, onNavigate = {}, showGuide = true) }
        listOf(
            AppStrings.navigationHome,
            AppStrings.navigationHistory,
            AppStrings.navigationInsights,
            GuideStrings.tabLabel,
            AppStrings.navigationSettings,
        ).forEach { composeRule.onNodeWithText(it).assertNotClipped(it, singleLine = true) }
    }
}
