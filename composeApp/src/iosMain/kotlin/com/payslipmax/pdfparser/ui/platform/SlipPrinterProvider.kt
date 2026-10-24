package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.part2.print.IosSlipPrinter
import com.payslipmax.pdfparser.part2.print.SlipPrinter

@Composable
actual fun rememberSlipPrinter(): SlipPrinter = remember { IosSlipPrinter() }
