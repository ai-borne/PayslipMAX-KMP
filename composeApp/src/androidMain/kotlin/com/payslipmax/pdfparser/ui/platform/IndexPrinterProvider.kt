package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.payslipmax.pdfparser.part2.print.AndroidIndexPrinter
import com.payslipmax.pdfparser.part2.print.IndexPrinter
import kotlinx.coroutines.Dispatchers

@Composable
actual fun rememberIndexPrinter(): IndexPrinter {
    val activity = LocalContext.current.findActivity()
    return remember(activity) { AndroidIndexPrinter(activity, Dispatchers.Main) }
}
