package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.part2.print.IndexPrinter
import com.payslipmax.pdfparser.part2.print.IosIndexPrinter

@Composable
actual fun rememberIndexPrinter(): IndexPrinter = remember { IosIndexPrinter() }
