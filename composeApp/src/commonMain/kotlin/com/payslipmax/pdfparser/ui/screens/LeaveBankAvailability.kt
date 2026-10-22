package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.subscription.LaunchFlags

/**
 * Whether the Leave Bank surfaces exist: the [LaunchFlags.LEAVE_BANK_ENABLED] kill switch and nothing else, in every build
 * type. Switched off, no surface shows and Retirement Calculators falls back to 300 leave days; the leave table is still parsed
 * and stored.
 */
fun isLeaveBankEnabled(): Boolean = LaunchFlags.LEAVE_BANK_ENABLED
