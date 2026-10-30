package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the officers who read these pages on the phone include people on the largest system font. At 2x on a 360 dp screen nothing the
 * pages say may be cut off (an ellipsis, or text lost at the bottom), and every control must have a name a screen reader can speak.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
// Real text shaping: the default (legacy) Robolectric graphics measure about 1px per character, so overflow is meaningless there.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2LearnA11yUiTest : Part2UiBase() {
    private fun SemanticsNodeInteraction.assertNotClipped(label: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        layouts.forEach { layout ->
            val ellipsized = (0 until layout.lineCount).any(layout::isLineEllipsized)
            assertFalse(ellipsized || layout.didOverflowHeight, "'$label' is cut off at 2x font on 360 dp")
        }
    }

    private fun assertEveryTextIsWhole(texts: List<String>) {
        texts.forEach { text ->
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().indices.forEach { i ->
                composeRule.onAllNodesWithText(text, substring = true)[i].assertNotClipped(text)
            }
        }
    }

    @Test
    fun theHomeTheRibbonAndTheCasualtyPageAreWholeAtTwiceTheFontOnASmallPhone() {
        fontScale = 2f
        val nav = show()
        tap(LearnStrings.entryTitle)
        assertEveryTextIsWhole(listOf(LearnStrings.entryTitle, LearnStrings.homeSubtitle, LearnStrings.searchHint, "The first ribbon", "The second ribbon"))
        tap("The first ribbon")
        assertEveryTextIsWhole(listOf("First case", "Second case", "Granted the first thing", "Reported for the second thing", "Released from the third thing"))
        tap("Granted the first thing")
        assertTrue(nav.current is Part2Destination.Code)
        assertEveryTextIsWhole(
            listOf(
                "Granted the first thing", LearnStrings.chapterAndPage(1, 28), LearnStrings.columnsHeading, "dd/mm/yyyy", LearnStrings.aboutHeading,
                "What it is.", "One thing to watch.", LearnStrings.relatedHeading, "Reported for the second thing", LearnStrings.relatedBasis(29), LearnStrings.addToBank,
            ),
        )
    }

    @Test
    fun everyControlOnTheLearnScreensHasANameAScreenReaderCanSpeak() {
        show()
        tap(LearnStrings.entryTitle)
        composeRule.onNodeWithText(LearnStrings.searchPlaceholder).assertIsDisplayed()
        type("fourth")
        composeRule.onAllNodesWithContentDescription(LearnStrings.searchClear).assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription(AppStrings.btnBack).assertCountEquals(1)
        composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
        tap("Granted the fourth thing")
        nameless().also { assertTrue(it.isEmpty(), "controls with no text or description on the page: $it") }
    }

    /** Every clickable node whose merged semantics carry neither text nor a content description. */
    private fun nameless(): List<String> =
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            val description = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            text.isEmpty() && description.isEmpty()
        }.map { it.id.toString() }

    @Test
    fun aListRowReadsAsOneButtonWithTheCasualtysNameInIt() {
        show()
        tap(LearnStrings.entryTitle)
        tap("The first ribbon")
        composeRule.onNode(hasText("Granted the first thing") and hasClickAction()).assertIsDisplayed()
    }
}
