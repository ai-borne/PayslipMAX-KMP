package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.part2.pages.PageScanner

/**
 * The platform's page scanner for the Part II vault, built from the screen the officer is looking at (on Android the scanner's intent is
 * started and answered through a screen's result launcher, which only composition has). It scans through the system document scanner and
 * nowhere else: photos only, nothing written to the gallery.
 */
@Composable
expect fun rememberPageScanner(): PageScanner
