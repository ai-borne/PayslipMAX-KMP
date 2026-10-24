package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.runtime.compositionLocalOf
import com.payslipmax.pdfparser.part2.print.SlipPrinter

/** A printer a test (or a host with its own) provides in place of the platform's; null means the platform's own, built from the screen. */
internal val LocalSlipPrinter = compositionLocalOf<SlipPrinter?> { null }
