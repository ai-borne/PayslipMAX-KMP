package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.payslipmax.pdfparser.part2.print.AndroidSlipPrinter
import com.payslipmax.pdfparser.part2.print.SlipPrinter
import kotlinx.coroutines.Dispatchers

@Composable
actual fun rememberSlipPrinter(): SlipPrinter {
    val activity = LocalContext.current.findActivity()
    return remember(activity) { AndroidSlipPrinter(activity, Dispatchers.Main) }
}
