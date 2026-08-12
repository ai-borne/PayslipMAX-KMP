package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

/**
 * Phase 8 (U2): the Tax Planner screen previously showed no indicator of which month's payslip is
 * driving its figures -- it silently inherits whatever `selectedPayslip` the Dashboard's month-picker
 * last set. This surfaces that state instead of leaving it implicit.
 */
@Composable
fun TaxDataAsOfBanner(
    activePayslipLabel: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${AppStringsTaxPlanner.dataAsOfPrefix}$activePayslipLabel",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}
