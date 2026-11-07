package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/** The header shortcut on the entry, attach and order screens: one tap to the Bank list instead of Back through each screen. Nothing when [onClick] is null. */
internal fun bankShortcut(onClick: (() -> Unit)?): (@Composable () -> Unit)? = onClick?.let { go -> { TextButton(onClick = go) { Text(OrdersBankStrings.toBank) } } }
