package com.payslipmax.pdfparser.domain

/**
 * Phase 5 — applies user-supplied per-field corrections *on read* over a parsed payslip.
 *
 * The corrected value is keyed by the same field key the parser uses in [ParsedPayslip.fieldConfidence]
 * (SSOT): a standardized [Earnings]/[Deductions] property name (e.g. `"basicPay"`, `"incomeTax"`,
 * `"miscEarnings"`) or, for genuinely-ambiguous line items, the raw label held in
 * [ParsedPayslip.rawEarnings]/[ParsedPayslip.rawDeductions].
 *
 * The original parse is never mutated: this returns a new [ParsedPayslip]. Printed totals
 * ([PayslipSummary]) are authoritative on the slip and are left untouched — a correction only fixes
 * the value of an individual line item the parser was unsure about. An empty map is a no-op.
 */
fun ParsedPayslip.applyCorrections(corrections: List<SingleCorrection>): ParsedPayslip {
    if (corrections.isEmpty()) return this
    var earnings = this.earnings
    var deductions = this.deductions
    val rawEarnings = this.rawEarnings.toMutableMap()
    val rawDeductions = this.rawDeductions.toMutableMap()

    for (correction in corrections) {
        val key = correction.fieldKey
        val value = correction.amount
        when (correction.type) {
            CorrectionType.DELETED -> {
                earnings = applyEarningsCorrection(earnings, key, 0.0) ?: earnings
                deductions = applyDeductionsCorrection(deductions, key, 0.0) ?: deductions
                rawEarnings.remove(key)
                rawDeductions.remove(key)
            }
            CorrectionType.EDITED -> {
                earnings = applyEarningsCorrection(earnings, key, value) ?: earnings
                deductions = applyDeductionsCorrection(deductions, key, value) ?: deductions
                if (key in rawEarnings) rawEarnings[key] = value
                if (key in rawDeductions) rawDeductions[key] = value
            }
            CorrectionType.ADDED -> {
                val code = correction.codeHead
                if (correction.category == EntryCategory.EARNING) {
                    val updated = applyEarningsCorrection(earnings, key, value) ?: applyEarningsCorrection(earnings, code, value)
                    if (updated != null) earnings = updated else rawEarnings[code] = value
                } else {
                    val updated = applyDeductionsCorrection(deductions, key, value) ?: applyDeductionsCorrection(deductions, code, value)
                    if (updated != null) deductions = updated else rawDeductions[code] = value
                }
            }
        }
    }
    return copy(
        earnings = earnings,
        deductions = deductions,
        rawEarnings = rawEarnings,
        rawDeductions = rawDeductions,
    )
}

fun ParsedPayslip.applyCorrections(corrections: Map<String, Double>): ParsedPayslip {
    if (corrections.isEmpty()) return this

    var earnings = this.earnings
    var deductions = this.deductions
    val rawEarnings = this.rawEarnings.toMutableMap()
    val rawDeductions = this.rawDeductions.toMutableMap()

    for ((key, value) in corrections) {
        val nextEarnings = applyEarningsCorrection(earnings, key, value)
        if (nextEarnings != null) {
            earnings = nextEarnings
            continue
        }
        val nextDeductions = applyDeductionsCorrection(deductions, key, value)
        if (nextDeductions != null) {
            deductions = nextDeductions
            continue
        }
        when (key) {
            in rawEarnings -> rawEarnings[key] = value
            in rawDeductions -> rawDeductions[key] = value
            else -> Unit // unknown key (e.g. field removed since the correction was stored) — ignore
        }
    }

    return copy(
        earnings = earnings,
        deductions = deductions,
        rawEarnings = rawEarnings,
        rawDeductions = rawDeductions,
    )
}

/** Returns a copy of [earnings] with [key] set to [value], or null if [key] is not an earnings field. */
private fun applyEarningsCorrection(
    earnings: Earnings,
    key: String,
    value: Double,
): Earnings? =
    when (key) {
        "basicPay" -> earnings.copy(basicPay = value)
        "dearnessAllowance" -> earnings.copy(dearnessAllowance = value)
        "militaryServicePay" -> earnings.copy(militaryServicePay = value)
        "transportAllowance" -> earnings.copy(transportAllowance = value)
        "transportAllowanceDa" -> earnings.copy(transportAllowanceDa = value)
        "dressAllowance" -> earnings.copy(dressAllowance = value)
        "rationMoney" -> earnings.copy(rationMoney = value)
        "specialForcesPay" -> earnings.copy(specialForcesPay = value)
        "fieldAllowance" -> earnings.copy(fieldAllowance = value)
        "childrenEducationAllowance" -> earnings.copy(childrenEducationAllowance = value)
        "houseRentAllowance" -> earnings.copy(houseRentAllowance = value)
        "riskHardshipAllowance" -> earnings.copy(riskHardshipAllowance = value)
        "nonPracticingAllowance" -> earnings.copy(nonPracticingAllowance = value)
        "adjBasicPay" -> earnings.copy(adjBasicPay = value)
        "adjDa" -> earnings.copy(adjDa = value)
        "adjMsp" -> earnings.copy(adjMsp = value)
        "adjTpta" -> earnings.copy(adjTpta = value)
        "arrearsCea" -> earnings.copy(arrearsCea = value)
        "arrearsDa" -> earnings.copy(arrearsDa = value)
        "arrearsRation" -> earnings.copy(arrearsRation = value)
        "arrearsSpecialForces" -> earnings.copy(arrearsSpecialForces = value)
        "arrearsTpta" -> earnings.copy(arrearsTpta = value)
        "arrearsTptaDa" -> earnings.copy(arrearsTptaDa = value)
        "arrearsHra" -> earnings.copy(arrearsHra = value)
        "arrearsRiskHardship" -> earnings.copy(arrearsRiskHardship = value)
        "adjPayAndAllce" -> earnings.copy(adjPayAndAllce = value)
        "adjFieldAllowance" -> earnings.copy(adjFieldAllowance = value)
        "medicalAllowance" -> earnings.copy(medicalAllowance = value)
        "adjTicketRecovery" -> earnings.copy(adjTicketRecovery = value)
        "miscEarnings" -> earnings.copy(miscEarnings = value)
        else -> null
    }

/** Returns a copy of [deductions] with [key] set to [value], or null if [key] is not a deductions field. */
private fun applyDeductionsCorrection(
    deductions: Deductions,
    key: String,
    value: Double,
): Deductions? =
    when (key) {
        "dsopSubscription" -> deductions.copy(dsopSubscription = value)
        "agif" -> deductions.copy(agif = value)
        "incomeTax" -> deductions.copy(incomeTax = value)
        "educationCess" -> deductions.copy(educationCess = value)
        "licenseFee" -> deductions.copy(licenseFee = value)
        "furnitureRent" -> deductions.copy(furnitureRent = value)
        "waterCharges" -> deductions.copy(waterCharges = value)
        "electricityCharges" -> deductions.copy(electricityCharges = value)
        "barrackDamage" -> deductions.copy(barrackDamage = value)
        "ticketRecovery" -> deductions.copy(ticketRecovery = value)
        "recFieldAllowance" -> deductions.copy(recFieldAllowance = value)
        "recSpecialForces" -> deductions.copy(recSpecialForces = value)
        "recoveryOfDebits" -> deductions.copy(recoveryOfDebits = value)
        "aobf" -> deductions.copy(aobf = value)
        "agifLoanRecovery" -> deductions.copy(agifLoanRecovery = value)
        "miscDeductions" -> deductions.copy(miscDeductions = value)
        else -> null
    }
