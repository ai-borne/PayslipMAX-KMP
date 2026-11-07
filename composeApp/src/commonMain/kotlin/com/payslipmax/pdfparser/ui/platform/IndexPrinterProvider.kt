package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.part2.print.IndexPrinter

/**
 * The platform's printer for the Part II orders index, built from the screen the officer is looking at (Android's print dialog needs an
 * Activity, which only composition has). It prints through the system dialog and nowhere else.
 */
@Composable
expect fun rememberIndexPrinter(): IndexPrinter
