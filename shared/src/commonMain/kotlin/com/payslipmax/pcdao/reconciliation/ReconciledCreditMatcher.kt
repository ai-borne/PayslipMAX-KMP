package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pdfparser.domain.ParsedPayslip

/**
 * Single source of truth for matching canonical entitlement keys to parsed credits.
 * Evaluates primary structured earnings, secondary arrears fields, and raw line items.
 */
class ReconciledCreditMatcher {
    fun getCreditedAmount(
        payslip: ParsedPayslip,
        key: String,
    ): Double {
        val structured = resolveStructuredAmount(payslip, key)
        if (structured > 0.0) return structured
        return resolveRawAmount(payslip.rawEarnings, key)
    }

    fun hasCeaCredit(payslip: ParsedPayslip): Boolean {
        if (payslip.earnings.childrenEducationAllowance > 0.0 || payslip.earnings.arrearsCea > 0.0) return true
        val rawMatch =
            findRawMatch(
                payslip.rawEarnings,
                "CEA", "C E A", "ARR-CEA", "A/o CEA", "RIMBCEAT", "RIMBCEAD", "HOSTEL", "Hostel Subsidy",
            )
        return rawMatch > 0.0
    }

    private fun resolveStructuredAmount(
        payslip: ParsedPayslip,
        key: String,
    ): Double {
        val e = payslip.earnings
        return when (key) {
            "TPTA" -> e.transportAllowance + e.transportAllowanceDa
            "HRA_SPR" -> e.houseRentAllowance
            "CEA", "HOSTEL_SUBSIDY" -> (e.childrenEducationAllowance + e.arrearsCea) / 12.0
            "HAFAA", "CFAA", "CMFAA", "SIACHEN" -> {
                if (e.riskHardshipAllowance > 0.0) e.riskHardshipAllowance else e.fieldAllowance
            }
            "NPA" -> e.nonPracticingAllowance
            "TECHNICAL_PAY" -> e.technicalAllowance
            "PARACHUTE_ALLOWANCE" -> e.specialForcesPay
            else -> 0.0
        }
    }

    private fun resolveRawAmount(
        raw: Map<String, Double>,
        key: String,
    ): Double {
        if (raw.isEmpty()) return 0.0
        return when (key) {
            "TPTA" -> findRawMatch(raw, "TPTA", "TRAN1", "TRAN-1", "TPTADA", "Tpt Allc", "Tpt DA")
            "HRA_SPR" -> findRawMatch(raw, "HRA", "HRAX", "HRAY", "HRAZ", "REIMACCO", "HH11", "HH12", "HH13")
            "CEA", "HOSTEL_SUBSIDY" -> findRawMatch(raw, "CEA", "ARR-CEA", "RIMBCEAT", "RIMBCEAD", "HOSTEL") / 12.0
            "HAFAA", "CFAA", "CMFAA", "SIACHEN" -> resolveRawRiskHardship(raw)
            "NPA" -> findRawMatch(raw, "NPA")
            "TECHNICAL_PAY" -> findRawMatch(raw, "TECI", "TECII", "ARR-TECI", "ARR-TECII", "TECHNICAL_PAY")
            "PARACHUTE_ALLOWANCE" -> findRawMatch(raw, "SPCDO", "PARA", "PARES", "PJI", "ARR-SPCDO")
            "SDA" -> findRawMatch(raw, "SDA", "ARR-SDA", "Special Duty Allowance")
            "ISDA" -> findRawMatch(raw, "ISDA", "ARR-ISDA", "Island SDA")
            "TRAINING_ALLOWANCE" -> findRawMatch(raw, "TRGALW", "TRAINING", "INSTR_ALW", "Instr Allce", "Instr Allowance")
            "LTC_ENCASHMENT" -> findRawMatch(raw, "LVELTC", "ARR-LVELTC", "LVENCASH", "LTC Encash")
            "CTG" -> findRawMatch(raw, "CTG", "C.T.G.", "BAGGAGE")
            else -> 0.0
        }
    }

    private fun resolveRawRiskHardship(raw: Map<String, Double>): Double =
        findRawMatch(
            raw,
            "RH11", "RH12", "RH13", "RH21", "RH22", "RH23", "RH31", "RH32", "RH33",
            "R1H1", "R1H2", "R1H3", "R2H1", "R2H2", "R2H3", "R3H1", "R3H2", "R3H3",
            "RHA", "ARR-RHA", "HAFA", "HAFAA", "CFAA", "CMFAA", "SIACHEN", "SICHA",
            "HAUCA", "HUACA", "SCCIA", "FD",
        )

    private fun findRawMatch(
        raw: Map<String, Double>,
        vararg aliases: String,
    ): Double {
        for (alias in aliases) {
            val direct = raw[alias]
            if (direct != null && direct > 0.0) return direct
        }
        for (alias in aliases) {
            val key = raw.keys.firstOrNull { it.equals(alias, ignoreCase = true) }
            if (key != null) {
                val amt = raw[key]
                if (amt != null && amt > 0.0) return amt
            }
        }
        return 0.0
    }
}
