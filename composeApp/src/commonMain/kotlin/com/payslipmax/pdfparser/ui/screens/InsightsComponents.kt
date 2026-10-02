package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.PayAuditWording

/** Display rupees: delegates to [PayAuditWording.rupees] so every screen and finding rounds and groups the same way. */
fun formatCurrency(amount: Double): String = PayAuditWording.rupees(amount)
