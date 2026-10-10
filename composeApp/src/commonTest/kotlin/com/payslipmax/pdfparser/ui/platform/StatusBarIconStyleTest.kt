package com.payslipmax.pdfparser.ui.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Why this matters: the status bar is transparent over the page, so its icons must contrast with the page.
// A dark page needs light icons and a light page needs dark icons — the pairing is what stops the clock
// and battery vanishing (white on white, found on the Pixel 9 in light theme).
class StatusBarIconStyleTest {
    @Test
    fun darkThemeGetsLightIcons() {
        assertEquals(StatusBarIconStyle.LightIcons, statusBarIconStyleFor(darkTheme = true))
    }

    @Test
    fun lightThemeGetsDarkIcons() {
        assertEquals(StatusBarIconStyle.DarkIcons, statusBarIconStyleFor(darkTheme = false))
    }

    @Test
    fun iconsAlwaysContrastWithThePage() {
        listOf(true, false).forEach { dark ->
            val style = statusBarIconStyleFor(dark)
            assertEquals(dark, !style.usesDarkIcons, "dark=$dark must never pair with $style")
        }
    }

    @Test
    fun darkIconsFlagMatchesTheStyle() {
        assertTrue(StatusBarIconStyle.DarkIcons.usesDarkIcons)
        assertFalse(StatusBarIconStyle.LightIcons.usesDarkIcons)
    }
}
