package com.payslipmax.pdfparser.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WHY: the Learn pages put grey secondary lines, label pills and accent text on the app's surfaces, and an officer reading in sunlight or
 * in the dark on a phone needs them legible in both themes. Each pair the Learn screens draw must reach the WCAG AA ratio for normal text
 * (4.5 to 1); the colours come from the theme, so this fails the day a token is changed to something faint.
 */
class LearnContrastTest {
    private fun ratio(
        a: Color,
        b: Color,
    ): Float {
        val (light, dark) = if (a.luminance() >= b.luminance()) a to b else b to a
        return (light.luminance() + FLARE) / (dark.luminance() + FLARE)
    }

    private fun pairs(scheme: ColorScheme): Map<String, Pair<Color, Color>> =
        mapOf(
            "page text on the background" to (scheme.onBackground to scheme.background),
            "row title on a card" to (scheme.onSurface to scheme.surface),
            "secondary line on a card" to (scheme.onSurfaceVariant to scheme.surface),
            "secondary line on the background" to (scheme.onSurfaceVariant to scheme.background),
            "label pill text on its pill" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "accent text on a card" to (scheme.primary to scheme.surface),
            "accent text on the background" to (scheme.primary to scheme.background),
        )

    private fun assertLegible(
        theme: String,
        scheme: ColorScheme,
    ) {
        for ((what, pair) in pairs(scheme)) {
            val value = ratio(pair.first, pair.second)
            assertTrue(value >= AA_NORMAL_TEXT, "$theme theme: $what is only ${(value * 10).toInt() / 10f} to 1, AA needs $AA_NORMAL_TEXT")
        }
    }

    @Test
    fun everyPairTheLearnScreensDrawIsLegibleInTheLightTheme() = assertLegible("light", AppLightColorScheme)

    @Test
    fun everyPairTheLearnScreensDrawIsLegibleInTheDarkTheme() = assertLegible("dark", AppDarkColorScheme)

    private companion object {
        const val AA_NORMAL_TEXT = 4.5f
        const val FLARE = 0.05f
    }
}
