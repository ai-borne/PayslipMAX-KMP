package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Why these matter: an officer who sets the phone's font to large must still read every tab name whole, and a screen title block
 * must not run into the screen edge. Both were seen on a Pixel at font scale 1.5 on a 360dp-wide screen (plan 22, P8): the bottom tabs
 * wrapped "Insight" / "s", and the Claim Guide's header subtitles ran flush to the right edge.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
// Real text shaping: the default (legacy) Robolectric graphics measure about 1px per character, so wrapping is meaningless there.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LargeFontLayoutTest {
    @AfterTest
    fun tearDown() {
        // RobolectricTestRunner starts Koin through PayslipApplication; stop it so it does not leak into the next class.
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Composable
    private fun NarrowLargeFont(
        width: Dp,
        content: @Composable () -> Unit,
    ) {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = LARGE_FONT)) {
            Box(Modifier.width(width)) { content() }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun headerSubtitleKeepsAGapFromTheScreenEdge() =
        runComposeUiTest {
            setContent { NarrowLargeFont(NARROW_SCREEN) { ScreenBackHeader(title = "Title", subtitle = LONG_SUBTITLE, onBack = {}) } }

            val right = onNodeWithText(LONG_SUBTITLE).getUnclippedBoundsInRoot().right
            assertTrue(right <= NARROW_SCREEN - AppDimensions.SpacingSmall, "subtitle ends at $right of $NARROW_SCREEN")
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun everyTabNameStaysOnOneLineAndWhole() =
        runComposeUiTest {
            setContent { NarrowLargeFont(NARROW_SCREEN) { AppBottomBar(currentScreen = Screen.Dashboard, onNavigate = {}, showGuide = true) } }

            val names = listOf(AppStrings.navigationHome, AppStrings.navigationHistory, AppStrings.navigationInsights, GuideStrings.tabLabel, AppStrings.navigationSettings)
            for (name in names) {
                val layouts = mutableListOf<TextLayoutResult>()
                onNodeWithText(name, useUnmergedTree = true).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                val layout = layouts.single()
                assertEquals(1, layout.lineCount, "$name wraps")
                assertFalse(layout.isLineEllipsized(0), "$name is cut short")
            }
        }

    private companion object {
        const val LARGE_FONT = 1.5f
        val NARROW_SCREEN = 360.dp

        // One unbreakable run of narrow letters: the text must wrap by character, so it fills the width it is given.
        val LONG_SUBTITLE = "i".repeat(200)
    }
}
