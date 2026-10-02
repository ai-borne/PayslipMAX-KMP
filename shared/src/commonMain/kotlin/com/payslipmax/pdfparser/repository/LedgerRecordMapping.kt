package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.AnomalySeverityMapper
import com.payslipmax.pdfparser.insights.toPersistedString

// Maps a payslip to its ledger row and an anomaly type to the persisted insight's category, title and severity.

internal fun ParsedPayslip.toLedgerRecordEntity(): LedgerRecordEntity {
    return LedgerRecordEntity(
        dateStr = dateStr,
        year = year,
        monthNum = monthNum,
        basicPay = earnings.basicPay,
        dearnessAllowance = earnings.dearnessAllowance,
        militaryServicePay = earnings.militaryServicePay,
        transportAllowance = earnings.transportAllowance,
        transportAllowanceDa = earnings.transportAllowanceDa,
        houseRentAllowance = earnings.houseRentAllowance,
        grossPay = summary.grossPay,
        dsopSubscription = deductions.dsopSubscription,
        incomeTax = deductions.incomeTax,
        netPay = summary.netRemittance,
        riskHardshipAllowance = earnings.riskHardshipAllowance,
        fieldAllowance = earnings.fieldAllowance,
        licenseFee = deductions.licenseFee,
        furnitureRent = deductions.furnitureRent,
        arrearsDa = earnings.arrearsDa,
        arrearsTpta = earnings.arrearsTpta,
        arrearsTptaDa = earnings.arrearsTptaDa,
        adjTpta = earnings.adjTpta,
        adjMsp = earnings.adjMsp,
        needsReview = needsReview,
    )
}

internal fun mapAnomalyTypeToCategory(type: String): String {
    return when (type) {
        "SALARY_LOSS", "DEBIT_RECOVERY", "INCREMENT_MISSED", "MSP_SHORTFALL" -> "SALARY_LOSS"
        "MISSING_ALLOWANCE", "TPTA_ENTITLEMENT", "ARREARS_AUDIT" -> "ALLOWANCE"
        "DEDUCTION_SPIKE", "RENT_RECOVERY_RISK", "TAX_PROJECTION" -> "TAX"
        "DSOP_COMPLIANCE", "DSOP_MILESTONE" -> "RETIREMENT"
        else -> "INFO"
    }
}

internal fun mapAnomalyTypeToTitle(type: String): String {
    return when (type) {
        "SALARY_LOSS" -> "Salary Reduction Detected"
        "MISSING_ALLOWANCE" -> "Missing Pay Allowance"
        "TPTA_ENTITLEMENT" -> "TPTA Entitlement Advisory"
        "DEDUCTION_SPIKE" -> "Deduction Spike Alert"
        "DSOP_COMPLIANCE" -> "DSOP Subscription Advisory"
        "RENT_RECOVERY_RISK" -> "Quarters Rent Recovery Risk"
        "DEBIT_RECOVERY" -> "Unexpected Debit Recovery"
        "DSOP_MILESTONE" -> "DSOP Milestone Credited"
        "TAX_PROJECTION" -> "Income Tax Cycle Projection"
        "ARREARS_AUDIT" -> "Dearness Allowance Arrears Verified"
        "INCREMENT_MISSED" -> "Annual Increment Not Applied"
        "MSP_SHORTFALL" -> "Military Service Pay Shortfall"
        else -> "Financial Advisory"
    }
}

internal fun mapAnomalyTypeToSeverity(type: String): String = AnomalySeverityMapper.severityOf(type).toPersistedString()
