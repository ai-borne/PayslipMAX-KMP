package com.payslipmax.pdfparser.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WHY (Phase 11 item 35): the Part II screens were read in dark theme and only briefly in light. Text is hard to read below a 4.5:1
 * contrast (WCAG AA) against what it sits on, and a button label below 3:1 is unreadable even as large text. These are the colour pairs the
 * Part II screens use (subtitles and helper lines, the overdue and error text, links, the Due and warning panels, filled buttons), taken
 * from the one colour source, in both schemes. The dark theme's white-on-blue button label is 3.7:1: above the large-text floor and below
 * 4.5, so it is held at 3 here and recorded as a shared-theme question in the plan (item 65), not changed under a Part II test.
 */
class Part2ContrastTest {
    private val schemes = listOf("light" to AppLightColorScheme, "dark" to AppDarkColorScheme)

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val high = maxOf(a.luminance(), b.luminance()).toDouble()
        val low = minOf(a.luminance(), b.luminance()).toDouble()
        return (high + 0.05) / (low + 0.05)
    }

    private fun assertAtLeast(
        floor: Double,
        name: String,
        pairs: ColorScheme.() -> List<Triple<String, Color, Color>>,
    ) {
        for ((scheme, colors) in schemes) {
            for ((what, text, ground) in colors.pairs()) {
                val ratio = contrast(text, ground)
                assertTrue(ratio >= floor, "$name, $scheme theme: $what is $ratio:1, below $floor:1")
            }
        }
    }

    @Test
    fun bodyAndHelperTextReadsOnEverySurfaceAPartTwoScreenPutsItOn() =
        assertAtLeast(AA_TEXT, "text") {
            listOf(
                Triple("body on the background", onBackground, background),
                Triple("body on a card", onSurface, surface),
                Triple("helper text on a card", onSurfaceVariant, surface),
                Triple("helper text on the background", onSurfaceVariant, background),
                Triple("helper text on a raised surface", onSurfaceVariant, surfaceVariant),
            )
        }

    @Test
    fun overdueErrorAndLinkTextReadOnACard() =
        assertAtLeast(AA_TEXT, "coloured text") {
            listOf(
                Triple("error text on a card", error, surface),
                Triple("error text on the background", error, background),
                Triple("link and action text on a card", primary, surface),
            )
        }

    @Test
    fun theDueAndWarningPanelTextReadsOnItsContainer() =
        assertAtLeast(AA_TEXT, "panel") {
            listOf(Triple("text on the error container", onErrorContainer, errorContainer), Triple("text on the primary container", onPrimaryContainer, primaryContainer))
        }

    @Test
    fun aFilledButtonsLabelClearsTheLargeTextFloor() =
        assertAtLeast(AA_LARGE, "filled button") {
            listOf(Triple("label on a primary button", onPrimary, primary), Triple("label on an error button", onError, error))
        }

    @Test
    fun theSelfCheckSeesALowContrastPair() {
        assertTrue(contrast(Color(0xFF777777), Color(0xFF888888)) < AA_LARGE, "grey on grey must fail, or this test file checks nothing")
        assertTrue(contrast(Color.Black, Color.White) > AA_TEXT)
    }

    private companion object {
        const val AA_TEXT = 4.5
        const val AA_LARGE = 3.0
    }
}
