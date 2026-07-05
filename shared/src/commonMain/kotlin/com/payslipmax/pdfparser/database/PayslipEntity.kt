package com.payslipmax.pdfparser.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.payslipmax.pdfparser.domain.*

// e.g. "01/2024" - acts as a unique primary key
@Entity(tableName = "payslips")
data class PayslipEntity(
    @PrimaryKey
    val dateStr: String,
    val file: String,
    val year: Int,
    val monthNum: Int,
    val monthName: String,
    @Embedded(prefix = "officer_")
    val officer: Officer,
    @Embedded(prefix = "earnings_")
    val earnings: Earnings,
    @Embedded(prefix = "deductions_")
    val deductions: Deductions,
    @Embedded(prefix = "ledger_")
    val ledgerBalances: LedgerBalances,
    @Embedded(prefix = "summary_")
    val summary: PayslipSummary,
    @Embedded(prefix = "tax_")
    val taxAndSavings: RoomTaxAndSavings?,
)

data class RoomTaxAndSavings(
    val grossSalaryYtd: Double = 0.0,
    val totalTaxableIncome: Double = 0.0,
    val standardDeduction: Double = 0.0,
    val netTaxableIncome: Double = 0.0,
    val totalTaxPayable: Double = 0.0,
    val taxDeductedYtd: Double = 0.0,
    val cessDeductedYtd: Double = 0.0,
    @Embedded(prefix = "dsop_")
    val dsopFund: DsopFund? = null,
)

// Helper extension functions to map between Entity and Domain model
fun ParsedPayslip.toEntity(): PayslipEntity {
    return PayslipEntity(
        dateStr = dateStr,
        file = file,
        year = year,
        monthNum = monthNum,
        monthName = monthName,
        officer = officer,
        earnings = earnings,
        deductions = deductions,
        ledgerBalances = ledgerBalances,
        summary = summary,
        taxAndSavings =
            taxAndSavings?.let {
                RoomTaxAndSavings(
                    grossSalaryYtd = it.grossSalaryYtd,
                    totalTaxableIncome = it.totalTaxableIncome,
                    standardDeduction = it.standardDeduction,
                    netTaxableIncome = it.netTaxableIncome,
                    totalTaxPayable = it.totalTaxPayable,
                    taxDeductedYtd = it.taxDeductedYtd,
                    cessDeductedYtd = it.cessDeductedYtd,
                    dsopFund = it.dsopFund,
                )
            },
    )
}

fun PayslipEntity.toDomain(): ParsedPayslip {
    return ParsedPayslip(
        file = file,
        year = year,
        monthNum = monthNum,
        monthName = monthName,
        dateStr = dateStr,
        officer = officer,
        earnings = earnings,
        deductions = deductions,
        ledgerBalances = ledgerBalances,
        summary = summary,
        taxAndSavings =
            taxAndSavings?.let {
                TaxAndSavings(
                    grossSalaryYtd = it.grossSalaryYtd,
                    totalTaxableIncome = it.totalTaxableIncome,
                    standardDeduction = it.standardDeduction,
                    netTaxableIncome = it.netTaxableIncome,
                    totalTaxPayable = it.totalTaxPayable,
                    taxDeductedYtd = it.taxDeductedYtd,
                    cessDeductedYtd = it.cessDeductedYtd,
                    dsopFund = it.dsopFund,
                )
            },
    )
}
