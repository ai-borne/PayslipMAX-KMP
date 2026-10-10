package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable

/** Which colour the system status bar icons (clock, battery, signal) are drawn in. */
enum class StatusBarIconStyle(val usesDarkIcons: Boolean) {
    LightIcons(usesDarkIcons = false),
    DarkIcons(usesDarkIcons = true),
}

/**
 * Single source of truth for the pairing: the status bar is transparent over the page, so a dark page
 * needs light icons and a light page needs dark icons. [darkTheme] is the EFFECTIVE app theme (the App
 * Appearance choice, not only the system mode), as resolved by `resolveDarkTheme`.
 */
fun statusBarIconStyleFor(darkTheme: Boolean): StatusBarIconStyle =
    if (darkTheme) StatusBarIconStyle.LightIcons else StatusBarIconStyle.DarkIcons

/**
 * Applies [style] to the hosting window's status bar. Called from `PDFParserTheme` so every Compose tree
 * that draws the theme also sets the icons that sit on top of it. Platform-specific because the status
 * bar belongs to the Activity window on Android and to the view controller on iOS.
 */
@Composable
expect fun ApplyStatusBarIconStyle(style: StatusBarIconStyle)
