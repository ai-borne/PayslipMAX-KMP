package com.payslipmax.pdfparser.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.part2.pages.IosPageScanner
import com.payslipmax.pdfparser.part2.pages.PageScanner

@Composable
actual fun rememberPageScanner(): PageScanner = remember { IosPageScanner() }
