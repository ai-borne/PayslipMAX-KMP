package com.payslipmax.pdfparser.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.ui.PayslipUiState
import com.payslipmax.pdfparser.ui.theme.GemmaModelStrings
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WHY (Phase 11 item 68, found on the emulator at 308dp wide with the font at 2.0): the download banner kept its title beside the Retry button
 * and the close button, so the title got a sliver of the row and wrapped one or two letters a line, filling the screen. Large fonts and
 * narrow phones are real, so the title must stay readable and every control must stay on screen, while the usual width keeps the one-row capsule.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BaseModelDownloadBannerLayoutUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val failed = PayslipUiState(modelDownloadError = "No space left on device")

    /** The banner on a screen [widthDp] wide (the emulator's 308 or a usual 412) with the font at [fontScale]. */
    private fun show(
        widthDp: Int,
        fontScale: Float,
        state: PayslipUiState = failed,
    ) = composeRule.setContent {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
            MaterialTheme { Box(Modifier.width(widthDp.dp)) { BaseModelDownloadBanner(uiState = state) } }
        }
    }

    private fun SemanticsNodeInteraction.lineCount(): Int {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().lineCount
    }

    private fun px(widthDp: Int) = widthDp * composeRule.activity.resources.displayMetrics.density

    @Test
    fun atTwiceTheFontOnANarrowPhoneTheTitleReadsInWholeWordsAndEveryControlIsOnScreen() {
        show(widthDp = 308, fontScale = 2f)
        val title = composeRule.onNodeWithText(GemmaModelStrings.gemmaModelPausedTitle)
        assertTrue(title.lineCount() <= 3, "the title wrapped to ${title.lineCount()} lines")
        for (control in listOf(
            composeRule.onNodeWithText(GemmaModelStrings.gemmaModelRetryAction),
            composeRule.onNodeWithContentDescription(GemmaModelStrings.gemmaModelDismissAction),
        )) {
            val bounds = control.fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= 0f && bounds.right <= px(308), "a control runs off the screen: $bounds")
        }
    }

    @Test
    fun atTheUsualFontTheTitleAndTheRetryButtonStayOnOneRow() {
        show(widthDp = 412, fontScale = 1f)
        val title = composeRule.onNodeWithText(GemmaModelStrings.gemmaModelPausedTitle).fetchSemanticsNode().boundsInRoot
        val retry = composeRule.onNodeWithText(GemmaModelStrings.gemmaModelRetryAction).fetchSemanticsNode().boundsInRoot
        assertTrue(retry.top < title.bottom && retry.bottom > title.top, "the button dropped below the title: title $title, retry $retry")
    }
}
