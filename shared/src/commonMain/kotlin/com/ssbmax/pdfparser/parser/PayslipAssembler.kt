package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.Deductions
import com.ssbmax.pdfparser.domain.Earnings
import com.ssbmax.pdfparser.domain.LedgerBalances
import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.domain.PayslipSummary
import com.ssbmax.pdfparser.domain.TaxAndSavings

/**
 * Assembles the final [ParsedPayslip] domain object from the classified credit/debit maps, the
 * reconciled totals, and the parsed metadata. Pure map→domain mapping extracted verbatim from
 * [PayslipTextParser] to keep that file within the 300-line limit (SSOT for domain construction).
 */
internal fun assembleParsedPayslip(
    filename: String,
    year: Int,
    monthNum: Int,
    monthName: String,
    dateStr: String,
    officer: Officer,
    earningsMap: Map<String, Double>,
    deductionsMap: Map<String, Double>,
    reconciled: ReconciledTotals,
    taxAndSavings: TaxAndSavings?,
    rawEarnings: Map<String, Double>,
    rawDeductions: Map<String, Double>,
): ParsedPayslip {
    val earnings = buildEarnings(earningsMap, reconciled.miscEarnings)
    val deductions = buildDeductions(deductionsMap, reconciled.miscDeductions)
    val ledgerBalances =
        LedgerBalances(
            openingCreditBalance = reconciled.ledger.openingCredit,
            openingDebitBalance = reconciled.ledger.openingDebit,
            closingCreditBalance = reconciled.ledger.closingCredit,
            closingDebitBalance = reconciled.ledger.closingDebit,
        )
    val summary =
        PayslipSummary(
            grossPay = reconciled.realGross,
            totalDeductions = reconciled.realDeductions,
            netRemittance = reconciled.finalNet,
        )

    return ParsedPayslip(
        file = filename,
        year = year,
        monthNum = monthNum,
        monthName = monthName,
        dateStr = dateStr,
        officer = officer,
        earnings = earnings,
        deductions = deductions,
        ledgerBalances = ledgerBalances,
        summary = summary,
        taxAndSavings = taxAndSavings,
        rawEarnings = rawEarnings,
        rawDeductions = rawDeductions,
    )
}

private fun buildEarnings(
    earningsMap: Map<String, Double>,
    miscEarnings: Double,
): Earnings =
    Earnings(
        basicPay = earningsMap["basicPay"] ?: 0.0,
        dearnessAllowance = earningsMap["dearnessAllowance"] ?: 0.0,
        militaryServicePay = earningsMap["militaryServicePay"] ?: 0.0,
        transportAllowance = earningsMap["transportAllowance"] ?: 0.0,
        transportAllowanceDa = earningsMap["transportAllowanceDa"] ?: 0.0,
        dressAllowance = earningsMap["dressAllowance"] ?: 0.0,
        rationMoney = earningsMap["rationMoney"] ?: 0.0,
        specialForcesPay = earningsMap["specialForcesPay"] ?: 0.0,
        fieldAllowance = earningsMap["fieldAllowance"] ?: 0.0,
        childrenEducationAllowance = earningsMap["childrenEducationAllowance"] ?: 0.0,
        houseRentAllowance = earningsMap["houseRentAllowance"] ?: 0.0,
        riskHardshipAllowance = earningsMap["riskHardshipAllowance"] ?: 0.0,
        nonPracticingAllowance = earningsMap["nonPracticingAllowance"] ?: 0.0,
        adjBasicPay = earningsMap["adjBasicPay"] ?: 0.0,
        adjDa = earningsMap["adjDa"] ?: 0.0,
        adjMsp = earningsMap["adjMsp"] ?: 0.0,
        adjTpta = earningsMap["adjTpta"] ?: 0.0,
        arrearsCea = earningsMap["arrearsCea"] ?: 0.0,
        arrearsDa = earningsMap["arrearsDa"] ?: 0.0,
        arrearsRation = earningsMap["arrearsRation"] ?: 0.0,
        arrearsSpecialForces = earningsMap["arrearsSpecialForces"] ?: 0.0,
        arrearsTpta = earningsMap["arrearsTpta"] ?: 0.0,
        arrearsTptaDa = earningsMap["arrearsTptaDa"] ?: 0.0,
        arrearsHra = earningsMap["arrearsHra"] ?: 0.0,
        arrearsRiskHardship = earningsMap["arrearsRiskHardship"] ?: 0.0,
        adjPayAndAllce = earningsMap["adjPayAndAllce"] ?: 0.0,
        adjFieldAllowance = earningsMap["adjFieldAllowance"] ?: 0.0,
        medicalAllowance = earningsMap["medicalAllowance"] ?: 0.0,
        adjTicketRecovery = earningsMap["adjTicketRecovery"] ?: 0.0,
        miscEarnings = miscEarnings,
    )

private fun buildDeductions(
    deductionsMap: Map<String, Double>,
    miscDeductions: Double,
): Deductions =
    Deductions(
        dsopSubscription = deductionsMap["dsopSubscription"] ?: 0.0,
        agif = deductionsMap["agif"] ?: 0.0,
        incomeTax = deductionsMap["incomeTax"] ?: 0.0,
        educationCess = deductionsMap["educationCess"] ?: 0.0,
        licenseFee = deductionsMap["licenseFee"] ?: 0.0,
        furnitureRent = deductionsMap["furnitureRent"] ?: 0.0,
        waterCharges = deductionsMap["waterCharges"] ?: 0.0,
        electricityCharges = deductionsMap["electricityCharges"] ?: 0.0,
        barrackDamage = deductionsMap["barrackDamage"] ?: 0.0,
        ticketRecovery = deductionsMap["ticketRecovery"] ?: 0.0,
        recFieldAllowance = deductionsMap["recFieldAllowance"] ?: 0.0,
        recSpecialForces = deductionsMap["recSpecialForces"] ?: 0.0,
        recoveryOfDebits = deductionsMap["recoveryOfDebits"] ?: 0.0,
        aobf = deductionsMap["aobf"] ?: 0.0,
        agifLoanRecovery = deductionsMap["agifLoanRecovery"] ?: 0.0,
        miscDeductions = miscDeductions,
    )
