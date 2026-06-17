package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.domain.ParsedPayslip

internal fun formatVal(value: Double): String {
    val longVal = value.toLong()
    val str = longVal.toString()
    if (str.length <= 3) return str
    val lastThree = str.substring(str.length - 3)
    val remaining = str.substring(0, str.length - 3)
    val builder = StringBuilder()
    var i = remaining.length
    while (i > 0) {
        if (i >= 2) {
            builder.insert(0, remaining.substring(i - 2, i))
            if (i - 2 > 0) builder.insert(0, ",")
            i -= 2
        } else {
            builder.insert(0, remaining.substring(0, 1))
            i -= 1
        }
    }
    return "$builder,$lastThree"
}

internal fun getCreditsList(payslip: ParsedPayslip): List<Triple<String, Double, String>> {
    val earnings = payslip.earnings
    return listOf(
        Triple("BPAY", earnings.basicPay, "Core salary based on rank and service years under 7th Pay Commission rules."),
        Triple(
            "MSP",
            earnings.militaryServicePay,
            "Military Service Pay. Compensates for hazardous and volatile lifestyle of military personnel.",
        ),
        Triple("DA", earnings.dearnessAllowance, "Dearness Allowance. Cost of living adjustment, revised twice a year."),
        Triple("TPTA", earnings.transportAllowance, "Transport Allowance. Commuting allowance based on duty station."),
        Triple("TPTADA", earnings.transportAllowanceDa, "Dearness Allowance computed on Transport Allowance amount."),
        Triple("RSHNA", earnings.rationMoney, "Ration Money Allowance. Dietary compensation when mess is not occupied."),
        Triple("DRESALW", earnings.dressAllowance, "Annual uniform allowance credited usually in July month."),
        Triple("SPCDO", earnings.specialForcesPay, "Special Forces hazard pay for commando or airborne units."),
        Triple("FD", earnings.fieldAllowance, "Field Area Allowance for deployment in active operational zones."),
        Triple("HRA", earnings.houseRentAllowance, "House Rent Allowance. Compensation for housing expenses when government quarters are not availed."),
        Triple("RHA", earnings.riskHardshipAllowance, "Risk & Hardship Allowance. Compensates for postings in difficult or operational areas."),
        Triple("NPA", earnings.nonPracticingAllowance, "Non-Practicing Allowance. Compensatory allowance for medical officers."),
        Triple("ARR-RHA", earnings.arrearsRiskHardship, "Arrears of Risk & Hardship Allowance."),
        Triple("ARR-HRA", earnings.arrearsHra, "Arrears of House Rent Allowance."),
        Triple("MISC", earnings.miscEarnings, "Miscellaneous unmapped credits or adjustment reconciliation difference."),
    ).filter { it.second != 0.0 }
}

internal fun getDebitsList(payslip: ParsedPayslip): List<Triple<String, Double, String>> {
    val deductions = payslip.deductions
    return listOf(
        Triple("DSOP", deductions.dsopSubscription, "Defence Services Officers Provident Fund. Tax-free retirement fund compound savings."),
        Triple("AGIF", deductions.agif, "Army Group Insurance Fund. Mandatory life cover and survival benefits contribution."),
        Triple("ITAX", deductions.incomeTax, "Income Tax deducted at source based on annual projections."),
        Triple("EHCESS", deductions.educationCess, "Health & Education Cess (4% of primary Income Tax amount)."),
        Triple("LF", deductions.licenseFee, "License Fee charged for occupying government married/single quarters."),
        Triple("FUR", deductions.furnitureRent, "Furniture Rent for government-provided appliances and items in quarters."),
        Triple("WATER", deductions.waterCharges, "Water supply charges for occupied quarters."),
        Triple("Elec", deductions.electricityCharges, "Electricity charges consumed in quarters."),
        Triple("Barrack Damage", deductions.barrackDamage, "Recoveries for damages or missing furniture items in quarters."),
        Triple("AOBF", deductions.aobf, "Army Officers Benevolent Fund. Recurring welfare contribution."),
        Triple("AGIF Loan", deductions.agifLoanRecovery, "Recovery of AGIF Car/Motorcycle loans."),
        Triple("MISC", deductions.miscDeductions, "Miscellaneous unmapped deductions or debit reconciliation difference."),
    ).filter { it.second != 0.0 }
}
