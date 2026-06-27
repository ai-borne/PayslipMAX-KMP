package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.parser.PayslipPatternConfig

private val creditsDescriptions =
    mapOf(
        "basicPay" to "Core salary based on rank and service years under 7th Pay Commission rules.",
        "militaryServicePay" to "Military Service Pay. Compensates for hazardous and volatile lifestyle of military personnel.",
        "dearnessAllowance" to "Dearness Allowance. Cost of living adjustment, revised twice a year.",
        "transportAllowance" to "Transport Allowance. Commuting allowance based on duty station.",
        "transportAllowanceDa" to "Dearness Allowance computed on Transport Allowance amount.",
        "rationMoney" to "Ration Money Allowance. Dietary compensation when mess is not occupied.",
        "dressAllowance" to "Outfit/Dress Allowance. Annual uniform allowance credited usually in July month.",
        "specialForcesPay" to "Special Forces hazard pay for commando or airborne units.",
        "fieldAllowance" to "Field Area Allowance for deployment in active operational zones.",
        "houseRentAllowance" to "House Rent Allowance. Compensation for housing expenses when government quarters are not availed.",
        "riskHardshipAllowance" to "Risk & Hardship Allowance. Compensates for postings in difficult or operational areas.",
        "nonPracticingAllowance" to "Non-Practicing Allowance. Compensatory allowance for medical officers.",
        "arrearsRiskHardship" to "Arrears of Risk & Hardship Allowance.",
        "arrearsHra" to "Arrears of House Rent Allowance.",
        "adjBasicPay" to "Adjustment of Basic Pay.",
        "adjDa" to "Adjustment of Dearness Allowance.",
        "adjMsp" to "Adjustment of Military Service Pay.",
        "adjTpta" to "Adjustment of Transport Allowance.",
        "adjPayAndAllce" to "Adjustment of Pay and Allowance.",
        "adjFieldAllowance" to "Adjustment of Field Allowance.",
        "medicalAllowance" to "Medical Allowance or Reimbursement.",
        "adjTicketRecovery" to "Adjustment of ticket recovery.",
        "miscEarnings" to "Miscellaneous unmapped credits or adjustment reconciliation difference.",
    )

private val debitsDescriptions =
    mapOf(
        "dsopSubscription" to "Defence Services Officers Provident Fund. Tax-free retirement fund compound savings.",
        "agif" to "Army Group Insurance Fund. Mandatory life cover and survival benefits contribution.",
        "incomeTax" to "Income Tax deducted at source based on annual projections.",
        "educationCess" to "Health & Education Cess (4% of primary Income Tax amount).",
        "licenseFee" to "License Fee charged for occupying government married/single quarters.",
        "furnitureRent" to "Furniture Rent for government-provided appliances and items in quarters.",
        "waterCharges" to "Water supply charges for occupied quarters.",
        "electricityCharges" to "Electricity charges consumed in quarters.",
        "barrackDamage" to "Recoveries for damages or missing furniture items in quarters.",
        "ticketRecovery" to "Recovery of ticket or travel charges.",
        "recFieldAllowance" to "Recovery of Field Allowance.",
        "recSpecialForces" to "Recovery of Special Forces Pay.",
        "recoveryOfDebits" to "Recovery of debits or other ledger balance elements.",
        "aobf" to "Army Officers Benevolent Fund. Recurring welfare contribution.",
        "agifLoanRecovery" to "Recovery of AGIF Car/Motorcycle loans.",
        "miscDeductions" to "Miscellaneous unmapped deductions or debit reconciliation difference.",
    )

private fun getCreditDesc(key: String): String {
    val stdKey = PayslipPatternConfig.creditKeysMapping[key] ?: key
    return creditsDescriptions[stdKey] ?: creditsDescriptions[key] ?: "Custom allowance or adjustment amount."
}

private fun getDebitDesc(key: String): String {
    val stdKey = PayslipPatternConfig.debitKeysMapping[key] ?: key
    return debitsDescriptions[stdKey] ?: debitsDescriptions[key] ?: "Custom deduction or recovery amount."
}

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
    if (payslip.rawEarnings.isEmpty()) {
        val earnings = payslip.earnings
        return listOf(
            Triple("BPAY", earnings.basicPay, getCreditDesc("BPAY")),
            Triple("MSP", earnings.militaryServicePay, getCreditDesc("MSP")),
            Triple("DA", earnings.dearnessAllowance, getCreditDesc("DA")),
            Triple("TPTA", earnings.transportAllowance, getCreditDesc("TPTA")),
            Triple("TPTADA", earnings.transportAllowanceDa, getCreditDesc("TPTADA")),
            Triple("RSHNA", earnings.rationMoney, getCreditDesc("RSHNA")),
            Triple("DRESALW", earnings.dressAllowance, getCreditDesc("DRESALW")),
            Triple("SPCDO", earnings.specialForcesPay, getCreditDesc("SPCDO")),
            Triple("FD", earnings.fieldAllowance, getCreditDesc("FD")),
            Triple("HRA", earnings.houseRentAllowance, getCreditDesc("HRA")),
            Triple("RHA", earnings.riskHardshipAllowance, getCreditDesc("RHA")),
            Triple("NPA", earnings.nonPracticingAllowance, getCreditDesc("NPA")),
            Triple("ARR-RHA", earnings.arrearsRiskHardship, getCreditDesc("ARR-RHA")),
            Triple("ARR-HRA", earnings.arrearsHra, getCreditDesc("ARR-HRA")),
            Triple("MISC", earnings.miscEarnings, getCreditDesc("miscEarnings")),
        ).filter { it.second != 0.0 }
    }

    val excluded = setOf("openingCreditBalance", "closingDebitBalance", "openingDebitBalance", "closingCreditBalance")
    return payslip.rawEarnings
        .filter { (key, value) ->
            value != 0.0 && (PayslipPatternConfig.creditKeysMapping[key] ?: key) !in excluded
        }
        .map { (key, value) ->
            Triple(key, value, getCreditDesc(key))
        }
}

internal fun getDebitsList(payslip: ParsedPayslip): List<Triple<String, Double, String>> {
    if (payslip.rawDeductions.isEmpty()) {
        val deductions = payslip.deductions
        return listOf(
            Triple("DSOP", deductions.dsopSubscription, getDebitDesc("DSOP")),
            Triple("AGIF", deductions.agif, getDebitDesc("AGIF")),
            Triple("ITAX", deductions.incomeTax, getDebitDesc("ITAX")),
            Triple("EHCESS", deductions.educationCess, getDebitDesc("EHCESS")),
            Triple("LF", deductions.licenseFee, getDebitDesc("LF")),
            Triple("FUR", deductions.furnitureRent, getDebitDesc("FUR")),
            Triple("WATER", deductions.waterCharges, getDebitDesc("WATER")),
            Triple("Elec", deductions.electricityCharges, getDebitDesc("Elec")),
            Triple("Barrack Damage", deductions.barrackDamage, getDebitDesc("Barrack Damage")),
            Triple("AOBF", deductions.aobf, getDebitDesc("AOBF")),
            Triple("AGIF Loan", deductions.agifLoanRecovery, getDebitDesc("AGIF Loan")),
            Triple("MISC", deductions.miscDeductions, getDebitDesc("miscDeductions")),
        ).filter { it.second != 0.0 }
    }

    val excluded = setOf("openingCreditBalance", "closingDebitBalance", "openingDebitBalance", "closingCreditBalance")
    return payslip.rawDeductions
        .filter { (key, value) ->
            value != 0.0 && (PayslipPatternConfig.debitKeysMapping[key] ?: key) !in excluded
        }
        .map { (key, value) ->
            Triple(key, value, getDebitDesc(key))
        }
}
