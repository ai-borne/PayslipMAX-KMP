package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.math.roundToInt

/** Builds a payslip whose DA is a whole percent of Basic + MSP, for the Pay Audit auditor tests. */
internal fun payAuditPayslip(
    year: Int,
    month: Int,
    basic: Double,
    daPercent: Double = 17.0,
    msp: Double = 15500.0,
    tpta: Double = 3600.0,
    hra: Double = 0.0,
    riskHardship: Double = 0.0,
    licenseFee: Double = 0.0,
    arrearsTpta: Double = 0.0,
) = ParsedPayslip(
    file = "t.pdf",
    year = year,
    monthNum = month,
    monthName = "",
    dateStr = "${month.toString().padStart(2, '0')}/$year",
    officer = Officer("N", "A", "P"),
    earnings =
        Earnings(
            basicPay = basic,
            militaryServicePay = msp,
            dearnessAllowance = ((basic + msp) * daPercent / 100.0).roundToInt().toDouble(),
            transportAllowance = tpta,
            houseRentAllowance = hra,
            riskHardshipAllowance = riskHardship,
            arrearsTpta = arrearsTpta,
        ),
    deductions = Deductions(licenseFee = licenseFee),
    ledgerBalances = LedgerBalances(),
    summary = PayslipSummary(0.0, 0.0, 0.0),
    taxAndSavings = null,
)

/** Consecutive monthly payslips from [from] (year, month) for [count] months, each built by [make]. */
internal fun payAuditMonths(
    fromYear: Int,
    fromMonth: Int,
    count: Int,
    make: (year: Int, month: Int) -> ParsedPayslip,
): List<ParsedPayslip> =
    (0 until count).map {
        val index = fromYear * 12 + fromMonth - 1 + it
        make(index / 12, index % 12 + 1)
    }
