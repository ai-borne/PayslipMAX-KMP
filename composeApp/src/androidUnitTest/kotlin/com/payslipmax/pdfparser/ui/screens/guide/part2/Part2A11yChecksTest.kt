package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the accessibility checks passed on the first run against the real screens, so they must be seen to fail on a control that is wrong.
 * Each case below is a small widget with exactly one fault (or none), so a check that silently looks at nothing is caught here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2A11yChecksTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val checks by lazy { Part2A11yChecks(composeRule) }

    private fun show(
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) = composeRule.setContent {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) { content() }
    }

    @Test
    fun aClickableWithNoTextAndNoDescriptionHasNoName() {
        show { Box(Modifier.size(48.dp).clickable {}) }
        assertEquals(1, checks.unnamedActions().size)
    }

    @Test
    fun aClickableWithADescriptionOrATextIsNamed() {
        show {
            Box(Modifier.size(48.dp).semantics { contentDescription = "Close" }.clickable {})
            Button(onClick = {}) { Text("Save") }
        }
        assertEquals(emptyList(), checks.unnamedActions())
    }

    @Test
    fun twoControlsWhoseTouchAreasOverlapAreReported() {
        show {
            Box(Modifier.size(48.dp).clickable {}.semantics { contentDescription = "A" })
            Box(Modifier.offset(y = 20.dp).size(48.dp).clickable {}.semantics { contentDescription = "B" })
        }
        assertEquals(1, checks.crowdedTargets().size)
    }

    @Test
    fun controlsSideBySideWithTheirTouchAreasJustMeetingAreNotReported() {
        show {
            Row {
                Box(Modifier.size(48.dp).clickable {}.semantics { contentDescription = "A" })
                Box(Modifier.size(48.dp).clickable {}.semantics { contentDescription = "B" })
            }
        }
        assertEquals(emptyList(), checks.crowdedTargets())
    }

    @Test
    fun aButtonLabelCutShortAtALargeFontIsReported() {
        show(fontScale = 2f) { Button(onClick = {}, modifier = Modifier.width(120.dp)) { Text("A long label that cannot fit", maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        assertTrue(checks.largeFontFaults().any { "cut short" in it }, checks.largeFontFaults().toString())
    }

    @Test
    fun aTextWiderThanTheScreenIsReported() {
        show(fontScale = 2f) { Text("Wider than the screen", modifier = Modifier.requiredWidth(500.dp)) }
        assertTrue(checks.largeFontFaults().any { "ends at" in it }, checks.largeFontFaults().toString())
    }

    @Test
    fun aShortLabelAtALargeFontPasses() {
        show(fontScale = 2f) { Button(onClick = {}) { Text("Save") } }
        assertEquals(emptyList(), checks.largeFontFaults())
    }
}
