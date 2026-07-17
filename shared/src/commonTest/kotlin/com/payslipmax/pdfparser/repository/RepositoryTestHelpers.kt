package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.domain.*

fun createMockPayslip(dateStr: String): ParsedPayslip {
    val split = dateStr.split("/")
    val month = split[0].toInt()
    val year = split[1].toInt()
    return ParsedPayslip(
        file = "payslip_$dateStr.pdf",
        year = year,
        monthNum = month,
        monthName = "Month_$month",
        dateStr = dateStr,
        officer = Officer("Name", "Acc", "PAN"),
        earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
        deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
        ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
        summary = PayslipSummary(100.0, 80.0, 20.0),
        taxAndSavings =
            TaxAndSavings(
                grossSalaryYtd = 1000.0,
                totalTaxableIncome = 900.0,
                standardDeduction = 50.0,
                netTaxableIncome = 850.0,
                totalTaxPayable = 100.0,
                taxDeductedYtd = 80.0,
                cessDeductedYtd = 20.0,
                dsopFund = DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0),
            ),
    )
}
