package com.payslipmax.pcdao.redressal

import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciliationResult
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.domain.ParsedPayslip

object RedressalLetterGenerator {
    const val PCDA_RECIPIENT =
        "The Principal Controller of Defence Accounts (Officers),\n" +
            "Golibar Maidan, Pune - 411001 (Maharashtra)"
    const val DEFAULT_SECTION_R = "Section R (Regimental Officers)"
    const val SECTION_L1 = "Section L-1 (Colonels & Brigadiers)"
    const val SECTION_M = "Section M (Medical Corps)"
    const val SECTION_T = "Section T (Transportation & Travel Claims)"

    const val CITATION_TLC = RedressalTemplates.CITATION_TLC
    const val CITATION_NPA = RedressalTemplates.CITATION_NPA
    const val CITATION_CFAA = RedressalTemplates.CITATION_CFAA
    const val CITATION_CMFAA = RedressalTemplates.CITATION_CMFAA
    const val CITATION_LEAVE_TPTA = RedressalTemplates.CITATION_LEAVE_TPTA

    fun generateLetter(
        request: RedressalRequest,
        currentDateStr: String = "28 September 2026",
    ): RedressalLetter {
        val totalDue = request.lineItems.sumOf { it.netDue }
        val id = CryptoHelper.sha256("${request.cdaAccountNo}-${request.disputeMonth}-$totalDue")

        val name = if (request.maskPii) maskName(request.officerName) else request.officerName
        val sNum = if (request.maskPii) maskServiceNumber(request.serviceNumber) else request.serviceNumber
        val cda = if (request.maskPii) maskCdaAccount(request.cdaAccountNo) else request.cdaAccountNo

        val subject = "FORMAL REPRESENTATION REGARDING DISCREPANCY IN RUNNING LEDGER ACCOUNT (IRLA)"
        val attention = "ATTENTION: ${request.ledgerSection}"
        val header = "CDA A/C  : $cda\nOFFICER  : ${request.rank} $name ($sNum)\nDATE     : $currentDateStr"

        val tableText = formatDiscrepancyTable(request.lineItems, request.exportFormat)
        val citationsText = formatStatutoryCitations(request.lineItems, request.exportFormat)
        val fullBody = buildFullBody(request, name, sNum, cda, currentDateStr, tableText, citationsText, totalDue)

        return RedressalLetter(
            id = id,
            recipient = PCDA_RECIPIENT,
            attention = attention,
            subject = subject,
            officerDetailsHeader = header,
            discrepancyTableText = tableText,
            statutoryCitationsText = citationsText,
            fullBodyText = fullBody,
            totalNetDue = totalDue,
            generatedDateStr = currentDateStr,
            isPiiMasked = request.maskPii,
            exportFormat = request.exportFormat,
            disputeMonth = request.disputeMonth,
            title = DEFAULT_REDRESSAL_TITLE,
        )
    }

    fun createRequestFromReconciliation(
        payslip: ParsedPayslip,
        reconciliationResult: ShadowLedgerReconciliationResult,
        rank: String = "Officer",
        serviceNumber: String = "IC-XXXXXX",
        ledgerSection: String? = null,
        maskPii: Boolean = false,
        exportFormat: ExportFormat = ExportFormat.TXT,
    ): RedressalRequest {
        val primaryRuleId = reconciliationResult.lineItems.firstOrNull()?.allowanceKey
        val resolvedSection = ledgerSection ?: resolveLedgerSection(rank, primaryRuleId)
        val lineItems =
            reconciliationResult.lineItems.mapIndexed { idx, diff ->
                DiffLineItem(
                    serialNo = idx + 1,
                    lineItemName = diff.allowanceName,
                    entitledAmount = diff.entitledAmount,
                    creditedAmount = diff.creditedAmount,
                    netDue = diff.netDifference,
                    statutoryAuthority = resolveStatutoryAuthority(diff.allowanceKey, diff.authorityRef),
                    ruleId = diff.allowanceKey,
                )
            }

        return RedressalRequest(
            officerName = payslip.officer.name.ifBlank { "Officer Name" },
            serviceNumber = serviceNumber,
            rank = rank,
            cdaAccountNo = payslip.officer.accountNo.ifBlank { "12/345/678901" },
            ledgerSection = resolvedSection,
            disputeMonth = payslip.dateStr.ifBlank { payslip.monthName + " " + payslip.year },
            lineItems = lineItems,
            maskPii = maskPii,
            pan = payslip.officer.pan,
            exportFormat = exportFormat,
        )
    }

    fun createRequestFromCumulativeRollup(
        payslip: ParsedPayslip,
        rollup: com.payslipmax.pcdao.timeline.CumulativeArrearsRollup,
        rank: String = "Officer",
        serviceNumber: String = "IC-XXXXXX",
        ledgerSection: String? = null,
        maskPii: Boolean = false,
        exportFormat: ExportFormat = ExportFormat.TXT,
    ): RedressalRequest {
        val resolvedSection = ledgerSection ?: resolveLedgerSection(rank)
        val lineItems =
            rollup.monthlyBreakdowns.mapIndexed { idx, item ->
                DiffLineItem(
                    serialNo = idx + 1,
                    lineItemName = "${item.monthName} ${item.year} - ${item.allowanceName}",
                    entitledAmount = item.entitledAmount,
                    creditedAmount = item.creditedAmount,
                    netDue = item.arrearsDue,
                    statutoryAuthority = "7th CPC & MoD Cumulative Entitlement Regulations",
                    ruleId = "CUMULATIVE_ARREARS",
                )
            }

        return RedressalRequest(
            officerName = payslip.officer.name.ifBlank { "Officer Name" },
            serviceNumber = serviceNumber,
            rank = rank,
            cdaAccountNo = payslip.officer.accountNo.ifBlank { "12/345/678901" },
            ledgerSection = resolvedSection,
            disputeMonth = "${rollup.auditedMonthCount} Months (${rollup.startMonthDateStr} to ${rollup.endMonthDateStr})",
            lineItems = lineItems,
            maskPii = maskPii,
            pan = payslip.officer.pan,
            exportFormat = exportFormat,
        )
    }

    fun createTlcDisallowanceRequest(
        payslip: ParsedPayslip,
        entitledHra: Double,
        rank: String = "Major",
        serviceNumber: String = "IC-XXXXXX",
        sprCityTier: String = "X",
    ): RedressalRequest =
        RedressalTemplates.createTlcDisallowanceRequest(
            payslip,
            entitledHra,
            rank,
            serviceNumber,
            sprCityTier,
        )

    fun createNpaOmissionRequest(
        payslip: ParsedPayslip,
        entitledNpa: Double,
        rank: String = "Captain",
        serviceNumber: String = "MS-XXXXXX",
    ): RedressalRequest =
        RedressalTemplates.createNpaOmissionRequest(
            payslip,
            entitledNpa,
            rank,
            serviceNumber,
        )

    fun createCfaaUnderpaymentRequest(
        payslip: ParsedPayslip,
        entitledCfaa: Double,
        creditedCfaa: Double = 0.0,
        rank: String = "Major",
        serviceNumber: String = "IC-XXXXXX",
        isModifiedField: Boolean = false,
    ): RedressalRequest =
        RedressalTemplates.createCfaaUnderpaymentRequest(
            payslip,
            entitledCfaa,
            creditedCfaa,
            rank,
            serviceNumber,
            isModifiedField,
        )

    fun createLeaveTptaWaiverRequest(
        payslip: ParsedPayslip,
        disputedDebitAmount: Double,
        rank: String = "Captain",
        serviceNumber: String = "IC-XXXXXX",
        dutyDaysPresent: Int = 1,
    ): RedressalRequest =
        RedressalTemplates.createLeaveTptaWaiverRequest(
            payslip,
            disputedDebitAmount,
            rank,
            serviceNumber,
            dutyDaysPresent,
        )

    fun resolveLedgerSection(
        rank: String,
        lineItemRuleId: String? = null,
    ): String {
        val upperRank = rank.uppercase()
        val upperRule = lineItemRuleId?.uppercase().orEmpty()
        return when {
            upperRank.contains("COLONEL") || upperRank.contains("BRIGADIER") ||
                upperRank.contains("GENERAL") -> SECTION_L1
            upperRank.contains("AMC") || upperRank.contains("ADC") || upperRank.contains("RVC") ||
                upperRank.contains("MNS") || upperRule.contains("NPA") -> SECTION_M
            upperRule.contains("TLC") || upperRule.contains("TWO_LOCATION") ||
                upperRule.contains("CONCESSION") || upperRule.contains("TPTA") ||
                upperRule.contains("TRAVEL") || upperRule.contains("LEAVE_TPTA") -> SECTION_T
            else -> DEFAULT_SECTION_R
        }
    }

    fun resolveStatutoryAuthority(
        allowanceKey: String,
        defaultAuthority: String = "",
    ): String {
        val upper = allowanceKey.uppercase()
        return when {
            upper.contains("TLC") || upper.contains("TWO_LOCATION") -> CITATION_TLC
            upper.contains("NPA") || upper.contains("AMC") -> CITATION_NPA
            upper.contains("CMFAA") -> CITATION_CMFAA
            upper.contains("CFAA") -> CITATION_CFAA
            upper.contains("LEAVE_TPTA") || (upper.contains("LEAVE") && upper.contains("TPTA")) -> CITATION_LEAVE_TPTA
            defaultAuthority.isNotBlank() -> defaultAuthority
            else -> "7th CPC & MoD Statutory Pay & Allowance Regulations"
        }
    }

    fun maskName(name: String): String = RedressalFormatters.maskName(name)

    fun maskServiceNumber(serviceNum: String): String = RedressalFormatters.maskServiceNumber(serviceNum)

    fun maskCdaAccount(cda: String): String = RedressalFormatters.maskCdaAccount(cda)

    fun maskPan(pan: String?): String? = RedressalFormatters.maskPan(pan)

    fun formatDiscrepancyTable(
        lineItems: List<DiffLineItem>,
        format: ExportFormat,
    ): String =
        RedressalFormatters.formatDiscrepancyTable(lineItems, format)

    fun formatStatutoryCitations(
        lineItems: List<DiffLineItem>,
        format: ExportFormat,
    ): String =
        RedressalFormatters.formatStatutoryCitations(lineItems, format)

    private fun buildFullBody(
        req: RedressalRequest,
        name: String,
        sNum: String,
        cda: String,
        dateStr: String,
        tableText: String,
        citationsText: String,
        totalDue: Double,
    ): String =
        RedressalFormatters.buildFullBody(
            req = req,
            name = name,
            sNum = sNum,
            cda = cda,
            dateStr = dateStr,
            tableText = tableText,
            citationsText = citationsText,
            totalDue = totalDue,
            recipient = PCDA_RECIPIENT,
        )
}
