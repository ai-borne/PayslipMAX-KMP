package com.payslipmax.pdfparser.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Regression guard for [CardTint]: pins the color each tint resolves to and the canonical border
 * alpha, so a future edit can't silently swap the Neutral/Accent mapping or collapse the
 * deliberate 0.1/0.2 alpha split this primitive introduced to replace the ad-hoc 0.08-0.3 drift
 * that existed across ~10 hand-rolled cards before extraction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FlatBorderedCardTintTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun neutralAndAccentTintsResolveToDistinctCanonicalColors() =
        runComposeUiTest {
            var neutralColor = Color.Unspecified
            var accentColor = Color.Unspecified
            var onSurface = Color.Unspecified
            var primary = Color.Unspecified

            setContent {
                MaterialTheme {
                    neutralColor = flatBorderedCardBorderColor(CardTint.Neutral)
                    accentColor = flatBorderedCardBorderColor(CardTint.Accent)
                    onSurface = MaterialTheme.colorScheme.onSurface
                    primary = MaterialTheme.colorScheme.primary
                }
            }

            // Neutral must stay derived from onSurface, Accent from primary.
            assertEquals(onSurface.copy(alpha = 1f), neutralColor.copy(alpha = 1f))
            assertEquals(primary.copy(alpha = 1f), accentColor.copy(alpha = 1f))
            assertNotEquals(neutralColor, accentColor)

            // Canonical alpha values collapsed from the ad-hoc 0.08-0.3 drift found across cards.
            // Tolerance covers Color's internal packed-float quantization (0.1f round-trips to
            // ~0.10196), not a meaningful drift window — a real alpha change (e.g. 0.1 -> 0.15)
            // still fails this.
            assertTrue(abs(neutralColor.alpha - 0.1f) < 0.01f, "Neutral border alpha drifted: ${neutralColor.alpha}")
            assertTrue(abs(accentColor.alpha - 0.2f) < 0.01f, "Accent border alpha drifted: ${accentColor.alpha}")
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun contentRendersForBothTintsAndBothPrimitiveVariants() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    FlatBorderedCard(tint = CardTint.Neutral) { Text("neutral-column-content") }
                    FlatBorderedCard(tint = CardTint.Accent) { Text("accent-column-content") }
                    FlatBorderedCardShape(tint = CardTint.Neutral) { Text("neutral-shape-content") }
                    FlatBorderedCardShape(tint = CardTint.Accent) { Text("accent-shape-content") }
                }
            }

            onNodeWithText("neutral-column-content").assertIsDisplayed()
            onNodeWithText("accent-column-content").assertIsDisplayed()
            onNodeWithText("neutral-shape-content").assertIsDisplayed()
            onNodeWithText("accent-shape-content").assertIsDisplayed()
        }
}
