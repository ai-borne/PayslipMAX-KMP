package com.payslipmax.pcdao.redressal

import com.payslipmax.pdfparser.domain.ParsedPayslip

object RedressalTemplates {
    const val CITATION_TLC =
        "Special Army Order (SAO 10/S/86) & PCDA(O) Handbook Chapter 10 Para 5 " +
            "(Concurrent Field Single Accommodation & Family SPR HRA)"
    const val CITATION_NPA =
        "MoD Letter No. 1(7)/2017/D(Pay/Services) dated 18 Sep 2017 " +
            "(20% NPA Compounding with DA & HRA within ₹2,37,500 Apex Ceiling)"
    const val CITATION_CFAA =
        "MoD Letter No. 8(3)/2000/D(Pay/Services) & MoD Letter No. 1(16)/2017/D(Pay/Services) " +
            "(Counter Insurgency & Field Area Allowances)"
    const val CITATION_CMFAA =
        "MoD Letter No. 1(16)/2017/D(Pay/Services) dated 18 Sep 2017 " +
            "(Compensatory Modified Field Area Allowance)"
    const val CITATION_LEAVE_TPTA =
        "Travel Regulations Rule 230(B) & MoD Letter No. 1(25)/2017/D(Pay/Services) " +
            "(TPTA Admissibility & Penal Interest Waiver for Partial Calendar Month)"

    fun createTlcDisallowanceRequest(
        payslip: ParsedPayslip,
        entitledHra: Double,
        rank: String = "Major",
        serviceNumber: String = "IC-XXXXXX",
        sprCityTier: String = "X",
    ): RedressalRequest {
        val item =
            DiffLineItem(
                serialNo = 1,
                lineItemName = "Two-Location Concession (TLC) SPR HRA (Tier $sprCityTier)",
                entitledAmount = entitledHra,
                creditedAmount = 0.0,
                netDue = entitledHra,
                statutoryAuthority = CITATION_TLC,
                ruleId = "TLC_DISALLOWANCE",
            )
        return RedressalRequest(
            officerName = payslip.officer.name.ifBlank { "Officer Name" },
            serviceNumber = serviceNumber,
            rank = rank,
            cdaAccountNo = payslip.officer.accountNo.ifBlank { "12/345/678901" },
            ledgerSection = RedressalLetterGenerator.resolveLedgerSection(rank, "TLC_DISALLOWANCE"),
            disputeMonth = payslip.dateStr.ifBlank { payslip.monthName + " " + payslip.year },
            lineItems = listOf(item),
            prayerRemarks =
                "Sanction concurrent credit of entitled HRA under Two-Location Concession (TLC) " +
                    "regulations for family residing at Selected Place of Residence (SPR) while serving in field area.",
            pan = payslip.officer.pan,
        )
    }

    fun createNpaOmissionRequest(
        payslip: ParsedPayslip,
        entitledNpa: Double,
        rank: String = "Captain",
        serviceNumber: String = "MS-XXXXXX",
    ): RedressalRequest {
        val item =
            DiffLineItem(
                serialNo = 1,
                lineItemName = "Non-Practicing Allowance (NPA @ 20%)",
                entitledAmount = entitledNpa,
                creditedAmount = 0.0,
                netDue = entitledNpa,
                statutoryAuthority = CITATION_NPA,
                ruleId = "AMC_NPA_OMISSION",
            )
        return RedressalRequest(
            officerName = payslip.officer.name.ifBlank { "Officer Name" },
            serviceNumber = serviceNumber,
            rank = rank,
            cdaAccountNo = payslip.officer.accountNo.ifBlank { "12/345/678901" },
            ledgerSection = RedressalLetterGenerator.resolveLedgerSection(rank, "AMC_NPA_OMISSION"),
            disputeMonth = payslip.dateStr.ifBlank { payslip.monthName + " " + payslip.year },
            lineItems = listOf(item),
            prayerRemarks =
                "Credit omitted 20% Non-Practicing Allowance (NPA) to IRLA and re-compute Dearness " +
                    "Allowance and HRA based on compounded Basic Pay + NPA.",
            pan = payslip.officer.pan,
        )
    }

    fun createCfaaUnderpaymentRequest(
        payslip: ParsedPayslip,
        entitledCfaa: Double,
        creditedCfaa: Double = 0.0,
        rank: String = "Major",
        serviceNumber: String = "IC-XXXXXX",
        isModifiedField: Boolean = false,
    ): RedressalRequest {
        val rule = if (isModifiedField) "CMFAA_UNDERPAYMENT" else "CFAA_UNDERPAYMENT"
        val name =
            if (isModifiedField) {
                "Compensatory Modified Field Allowance (CMFAA)"
            } else {
                "Counter Insurgency / Field Allowance (CFAA)"
            }
        val authority = if (isModifiedField) CITATION_CMFAA else CITATION_CFAA
        val item =
            DiffLineItem(
                serialNo = 1,
                lineItemName = name,
                entitledAmount = entitledCfaa,
                creditedAmount = creditedCfaa,
                netDue = entitledCfaa - creditedCfaa,
                statutoryAuthority = authority,
                ruleId = rule,
            )
        return RedressalRequest(
            officerName = payslip.officer.name.ifBlank { "Officer Name" },
            serviceNumber = serviceNumber,
            rank = rank,
            cdaAccountNo = payslip.officer.accountNo.ifBlank { "12/345/678901" },
            ledgerSection = RedressalLetterGenerator.resolveLedgerSection(rank, rule),
            disputeMonth = payslip.dateStr.ifBlank { payslip.monthName + " " + payslip.year },
            lineItems = listOf(item),
            prayerRemarks =
                "Credit arrears of entitled field allowance per 7th CPC Risk & Hardship Matrix with " +
                    "admissible DA escalation.",
            pan = payslip.officer.pan,
        )
    }

    fun createLeaveTptaWaiverRequest(
        payslip: ParsedPayslip,
        disputedDebitAmount: Double,
        rank: String = "Captain",
        serviceNumber: String = "IC-XXXXXX",
        dutyDaysPresent: Int = 1,
    ): RedressalRequest {
        val item =
            DiffLineItem(
                serialNo = 1,
                lineItemName = "Transport Allowance (TPTA) Absence Recovery Waiver",
                entitledAmount = disputedDebitAmount,
                creditedAmount = 0.0,
                netDue = disputedDebitAmount,
                statutoryAuthority = CITATION_LEAVE_TPTA,
                ruleId = "LEAVE_TPTA_WAIVER",
            )
        return RedressalRequest(
            officerName = payslip.officer.name.ifBlank { "Officer Name" },
            serviceNumber = serviceNumber,
            rank = rank,
            cdaAccountNo = payslip.officer.accountNo.ifBlank { "12/345/678901" },
            ledgerSection = RedressalLetterGenerator.resolveLedgerSection(rank, "LEAVE_TPTA_WAIVER"),
            disputeMonth = payslip.dateStr.ifBlank { payslip.monthName + " " + payslip.year },
            lineItems = listOf(item),
            prayerRemarks =
                "Cancel recovery of Transport Allowance and refund statutory debit. As confirmed by " +
                    "Part II Order, applicant performed duty on $dutyDaysPresent day(s) during the month and was not " +
                    "absent for the entire calendar month.",
            pan = payslip.officer.pan,
        )
    }
}
