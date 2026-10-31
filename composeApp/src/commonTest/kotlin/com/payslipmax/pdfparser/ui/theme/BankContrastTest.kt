package com.payslipmax.pdfparser.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WHY: the Bank's whole message is two coloured words, orange (waiting) and blue (attached), read on a card, often outdoors or at night.
 * Each colour, in each theme, must reach the WCAG AA ratio for normal text (4.5 to 1) on the surface and the background it is drawn on, and
 * the two must stay apart from each other, or the officer cannot tell waiting from done.
 */
class BankContrastTest {
    private fun ratio(
        a: Color,
        b: Color,
    ): Float {
        val (light, dark) = if (a.luminance() >= b.luminance()) a to b else b to a
        return (light.luminance() + FLARE) / (dark.luminance() + FLARE)
    }

    private fun assertLegible(
        theme: String,
        colours: Map<String, Color>,
        surfaces: Map<String, Color>,
    ) {
        for ((name, colour) in colours) {
            for ((where, surface) in surfaces) {
                val value = ratio(colour, surface)
                assertTrue(value >= AA_NORMAL_TEXT, "$theme theme: $name on $where is only ${(value * 10).toInt() / 10f} to 1, AA needs $AA_NORMAL_TEXT")
            }
        }
    }

    @Test
    fun waitingAndAttachedAreLegibleInTheLightTheme() =
        assertLegible(
            "light",
            mapOf("waiting" to BankColors.WaitingLight, "attached" to BankColors.AttachedLight),
            mapOf("a card" to AppLightColorScheme.surface, "the background" to AppLightColorScheme.background),
        )

    @Test
    fun waitingAndAttachedAreLegibleInTheDarkTheme() =
        assertLegible(
            "dark",
            mapOf("waiting" to BankColors.WaitingDark, "attached" to BankColors.AttachedDark),
            mapOf("a card" to AppDarkColorScheme.surface, "the background" to AppDarkColorScheme.background),
        )

    @Test
    fun theTwoStatusesStayDistinctInBothThemes() {
        assertTrue(BankColors.WaitingLight.red > BankColors.WaitingLight.blue, "waiting is the warm one")
        assertTrue(BankColors.AttachedLight.blue > BankColors.AttachedLight.red, "attached is the cool one")
        assertTrue(BankColors.WaitingDark.red > BankColors.WaitingDark.blue && BankColors.AttachedDark.blue > BankColors.AttachedDark.red)
    }

    private companion object {
        const val AA_NORMAL_TEXT = 4.5f
        const val FLARE = 0.05f
    }
}
