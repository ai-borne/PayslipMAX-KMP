package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.domain.ConfidenceThresholds
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.parser.PayslipPatternConfig

// Pre-computed set of lower-cased Hindi transliteration words for display-layer filtering.
// Defensive guard: parser-level filtering handles new parses; this catches any that slipped
// through in old DB data or edge-case token layouts.
private val hindiWordSet: Set<String> = PayslipPatternConfig.hindiTransliterations.map { it.lowercase() }.toSet()

/** True when a raw map key is a valid payslip code — has at least one letter after stripping Hindi words. */
private fun isValidRawKey(key: String): Boolean {
    val afterHindi = key.lowercase().split(Regex("\\s+")).filterNot { it in hindiWordSet }.joinToString(" ").trim()
    return afterHindi.any { it.isLetter() }
}

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

/**
 * One displayed ledger row. [fieldKey] is the SSOT key the parser uses in
 * [ParsedPayslip.fieldConfidence] and that the Phase 5 correction flow persists against — a
 * standardized domain field name for structured rows, or the raw label for ambiguous ones.
 */
internal data class LedgerLine(
    val code: String,
    val amount: Double,
    val desc: String,
    val fieldKey: String,
)

internal fun getCreditsList(payslip: ParsedPayslip): List<LedgerLine> {
    val items =
        if (payslip.rawEarnings.isEmpty()) {
            val earnings = payslip.earnings
            listOf(
                LedgerLine("BPAY", earnings.basicPay, getCreditDesc("BPAY"), "basicPay"),
                LedgerLine("MSP", earnings.militaryServicePay, getCreditDesc("MSP"), "militaryServicePay"),
                LedgerLine("DA", earnings.dearnessAllowance, getCreditDesc("DA"), "dearnessAllowance"),
                LedgerLine("TPTA", earnings.transportAllowance, getCreditDesc("TPTA"), "transportAllowance"),
                LedgerLine("TPTADA", earnings.transportAllowanceDa, getCreditDesc("TPTADA"), "transportAllowanceDa"),
                LedgerLine("RSHNA", earnings.rationMoney, getCreditDesc("RSHNA"), "rationMoney"),
                LedgerLine("DRESALW", earnings.dressAllowance, getCreditDesc("DRESALW"), "dressAllowance"),
                LedgerLine("SPCDO", earnings.specialForcesPay, getCreditDesc("SPCDO"), "specialForcesPay"),
                LedgerLine("FD", earnings.fieldAllowance, getCreditDesc("FD"), "fieldAllowance"),
                LedgerLine("CEA", earnings.childrenEducationAllowance, getCreditDesc("CEA"), "childrenEducationAllowance"),
                LedgerLine("HRA", earnings.houseRentAllowance, getCreditDesc("HRA"), "houseRentAllowance"),
                LedgerLine("RHA", earnings.riskHardshipAllowance, getCreditDesc("RHA"), "riskHardshipAllowance"),
                LedgerLine("NPA", earnings.nonPracticingAllowance, getCreditDesc("NPA"), "nonPracticingAllowance"),
                LedgerLine("A/o BPAY", earnings.adjBasicPay, getCreditDesc("adjBasicPay"), "adjBasicPay"),
                LedgerLine("A/o DA-", earnings.adjDa, getCreditDesc("adjDa"), "adjDa"),
                LedgerLine("A/o MSP-", earnings.adjMsp, getCreditDesc("adjMsp"), "adjMsp"),
                LedgerLine("A/o TPTA", earnings.adjTpta, getCreditDesc("adjTpta"), "adjTpta"),
                LedgerLine("ARR-CEA", earnings.arrearsCea, getCreditDesc("arrearsCea"), "arrearsCea"),
                LedgerLine("A/o DA", earnings.arrearsDa, getCreditDesc("arrearsDa"), "arrearsDa"),
                LedgerLine("ARR-RSHNA", earnings.arrearsRation, getCreditDesc("ARR-RSHNA"), "arrearsRation"),
                LedgerLine("ARR-SPCDO", earnings.arrearsSpecialForces, getCreditDesc("ARR-SPCDO"), "arrearsSpecialForces"),
                LedgerLine("ARR-TPTA", earnings.arrearsTpta, getCreditDesc("ARR-TPTA"), "arrearsTpta"),
                LedgerLine("ARR-TPTADA", earnings.arrearsTptaDa, getCreditDesc("ARR-TPTADA"), "arrearsTptaDa"),
                LedgerLine("ARR-HRA", earnings.arrearsHra, getCreditDesc("ARR-HRA"), "arrearsHra"),
                LedgerLine("ARR-RHA", earnings.arrearsRiskHardship, getCreditDesc("ARR-RHA"), "arrearsRiskHardship"),
                LedgerLine("ADJ P&A", earnings.adjPayAndAllce, getCreditDesc("adjPayAndAllce"), "adjPayAndAllce"),
                LedgerLine("A/o FD", earnings.adjFieldAllowance, getCreditDesc("adjFieldAllowance"), "adjFieldAllowance"),
                LedgerLine("MEDICAL", earnings.medicalAllowance, getCreditDesc("medicalAllowance"), "medicalAllowance"),
                LedgerLine("ETKT-REF", earnings.adjTicketRecovery, getCreditDesc("adjTicketRecovery"), "adjTicketRecovery"),
                LedgerLine("MISC", earnings.miscEarnings, getCreditDesc("miscEarnings"), "miscEarnings"),
            ).filter { it.amount != 0.0 }
        } else {
            val excluded = setOf("openingCreditBalance", "closingDebitBalance", "openingDebitBalance", "closingCreditBalance")
            payslip.rawEarnings
                .filter { (key, value) ->
                    value != 0.0 &&
                        (PayslipPatternConfig.creditKeysMapping[key] ?: key) !in excluded &&
                        isValidRawKey(key)
                }
                .map { (key, value) -> LedgerLine(key, value, getCreditDesc(key), key) }
        }

    val totalItemSum = items.sumOf { it.amount }
    val residual = payslip.summary.grossPay - totalItemSum
    return if (residual > ConfidenceThresholds.ITEM_SUM_TOLERANCE && items.none { it.code == "MISC" }) {
        items + LedgerLine("MISC", residual, getCreditDesc("miscEarnings"), "miscEarnings")
    } else {
        items
    }
}

internal fun getDebitsList(payslip: ParsedPayslip): List<LedgerLine> {
    val items =
        if (payslip.rawDeductions.isEmpty()) {
            val deductions = payslip.deductions
            listOf(
                LedgerLine("DSOP", deductions.dsopSubscription, getDebitDesc("DSOP"), "dsopSubscription"),
                LedgerLine("AGIF", deductions.agif, getDebitDesc("AGIF"), "agif"),
                LedgerLine("ITAX", deductions.incomeTax, getDebitDesc("ITAX"), "incomeTax"),
                LedgerLine("EHCESS", deductions.educationCess, getDebitDesc("EHCESS"), "educationCess"),
                LedgerLine("LF", deductions.licenseFee, getDebitDesc("LF"), "licenseFee"),
                LedgerLine("FUR", deductions.furnitureRent, getDebitDesc("FUR"), "furnitureRent"),
                LedgerLine("WATER", deductions.waterCharges, getDebitDesc("WATER"), "waterCharges"),
                LedgerLine("Elec", deductions.electricityCharges, getDebitDesc("Elec"), "electricityCharges"),
                LedgerLine("Barrack Damage", deductions.barrackDamage, getDebitDesc("Barrack Damage"), "barrackDamage"),
                LedgerLine("ETKT", deductions.ticketRecovery, getDebitDesc("ETKT"), "ticketRecovery"),
                LedgerLine("Rec FD", deductions.recFieldAllowance, getDebitDesc("Rec FD"), "recFieldAllowance"),
                LedgerLine("Rec SPCDO", deductions.recSpecialForces, getDebitDesc("Rec SPCDO"), "recSpecialForces"),
                LedgerLine("Recv P&A", deductions.recoveryOfDebits, getDebitDesc("Recv P&A"), "recoveryOfDebits"),
                LedgerLine("AOBF", deductions.aobf, getDebitDesc("AOBF"), "aobf"),
                LedgerLine("AGIF Loan", deductions.agifLoanRecovery, getDebitDesc("AGIF Loan"), "agifLoanRecovery"),
                LedgerLine("MISC", deductions.miscDeductions, getDebitDesc("miscDeductions"), "miscDeductions"),
            ).filter { it.amount != 0.0 }
        } else {
            val excluded = setOf("openingCreditBalance", "closingDebitBalance", "openingDebitBalance", "closingCreditBalance")
            payslip.rawDeductions
                .filter { (key, value) ->
                    value != 0.0 &&
                        (PayslipPatternConfig.debitKeysMapping[key] ?: key) !in excluded &&
                        isValidRawKey(key)
                }
                .map { (key, value) -> LedgerLine(key, value, getDebitDesc(key), key) }
        }

    val totalItemSum = items.sumOf { it.amount }
    val residual = payslip.summary.totalDeductions - totalItemSum
    return if (residual > ConfidenceThresholds.ITEM_SUM_TOLERANCE && items.none { it.code == "MISC" }) {
        items + LedgerLine("MISC", residual, getDebitDesc("miscDeductions"), "miscDeductions")
    } else {
        items
    }
}

/** Positive when displayed credit items sum exceeds the printed Gross Pay (phantom entry detected). */
internal fun creditsMismatch(payslip: ParsedPayslip): Double =
    getCreditsList(payslip).sumOf { it.amount } - payslip.summary.grossPay

/** Positive when displayed debit items sum exceeds the printed Total Deductions (phantom entry detected). */
internal fun debitsMismatch(payslip: ParsedPayslip): Double =
    getDebitsList(payslip).sumOf { it.amount } - payslip.summary.totalDeductions
