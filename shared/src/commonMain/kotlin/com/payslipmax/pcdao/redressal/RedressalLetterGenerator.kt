package com.payslipmax.pcdao.redressal

import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciliationResult
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.domain.ParsedPayslip

object RedressalLetterGenerator {
    private const val PCDA_RECIPIENT = "The Principal Controller of Defence Accounts (Officers),\nGolibar Maidan, Pune - 411001 (Maharashtra)"
    private const val DEFAULT_SECTION_R = "Section R (Regimental Officers)"
    private const val SECTION_L1 = "Section L-1 (Colonels & Brigadiers)"
    private const val SECTION_M = "Section M (Medical Corps)"

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
        val resolvedSection = ledgerSection ?: resolveLedgerSection(rank)
        val lineItems =
            reconciliationResult.lineItems.mapIndexed { idx, diff ->
                DiffLineItem(
                    serialNo = idx + 1,
                    lineItemName = diff.allowanceName,
                    entitledAmount = diff.entitledAmount,
                    creditedAmount = diff.creditedAmount,
                    netDue = diff.netDifference,
                    statutoryAuthority = diff.authorityRef,
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

    fun resolveLedgerSection(rank: String): String {
        val upper = rank.uppercase()
        return when {
            upper.contains("COLONEL") || upper.contains("BRIGADIER") || upper.contains("GENERAL") -> SECTION_L1
            upper.contains("AMC") || upper.contains("ADC") || upper.contains("RVC") || upper.contains("MNS") -> SECTION_M
            else -> DEFAULT_SECTION_R
        }
    }

    fun maskName(name: String): String {
        if (name.isBlank()) return "****"
        val trimmed = name.trim()
        val firstChar = trimmed.first()
        return "$firstChar. *******"
    }

    fun maskServiceNumber(serviceNum: String): String {
        if (serviceNum.length <= 4) return "****"
        val visiblePart = serviceNum.take(serviceNum.length.coerceAtMost(5))
        return "$visiblePart***"
    }

    fun maskCdaAccount(cda: String): String {
        if (cda.length <= 5) return "******"
        return cda.take(5) + "******"
    }

    fun maskPan(pan: String?): String? {
        if (pan.isNullOrBlank()) return null
        if (pan.length < 10) return "******"
        return pan.take(5) + "****" + pan.takeLast(1)
    }

    fun formatDiscrepancyTable(
        lineItems: List<DiffLineItem>,
        format: ExportFormat,
    ): String {
        return if (format == ExportFormat.MARKDOWN) {
            formatMarkdownTable(lineItems)
        } else {
            formatTextTable(lineItems)
        }
    }

    private fun formatTextTable(lineItems: List<DiffLineItem>): String {
        val sb = StringBuilder()
        sb.appendLine("SR | DISCREPANCY LINE ITEM         | ENTITLED   | CREDITED   | NET DUE  ")
        sb.appendLine("-----------------------------------------------------------------------")
        if (lineItems.isEmpty()) {
            sb.appendLine("N/A| No discrepancies detected     | Rs. 0      | Rs. 0      | Rs. 0   ")
        } else {
            lineItems.forEach { item ->
                val sr = item.serialNo.toString().padEnd(2)
                val name = item.lineItemName.take(28).padEnd(28)
                val ent = formatCurrency(item.entitledAmount).padEnd(10)
                val cred = formatCurrency(item.creditedAmount).padEnd(10)
                val due = formatCurrency(item.netDue).padEnd(9)
                sb.appendLine("$sr | $name | $ent | $cred | $due")
            }
        }
        sb.appendLine("-----------------------------------------------------------------------")
        val total = lineItems.sumOf { it.netDue }
        sb.append("TOTAL STATUTORY NET DUE: ${formatCurrency(total)}")
        return sb.toString()
    }

    private fun formatMarkdownTable(lineItems: List<DiffLineItem>): String {
        val sb = StringBuilder()
        sb.appendLine("| SR. | DISCREPANCY LINE ITEM | ENTITLED | CREDITED | NET DUE |")
        sb.appendLine("| :--- | :--- | :--- | :--- | :--- |")
        lineItems.forEach { item ->
            sb.appendLine("| ${item.serialNo} | ${item.lineItemName} | ${formatCurrency(item.entitledAmount)} | ${formatCurrency(item.creditedAmount)} | ${formatCurrency(item.netDue)} |")
        }
        val total = lineItems.sumOf { it.netDue }
        sb.append("\n**TOTAL STATUTORY NET DUE**: ${formatCurrency(total)}")
        return sb.toString()
    }

    fun formatStatutoryCitations(
        lineItems: List<DiffLineItem>,
        format: ExportFormat,
    ): String {
        if (lineItems.isEmpty()) return "None"
        val sb = StringBuilder()
        lineItems.forEachIndexed { idx, item ->
            val num = ('a' + (idx % 26)).toString()
            if (format == ExportFormat.MARKDOWN) {
                sb.appendLine("- **${item.lineItemName}**: ${item.statutoryAuthority}")
            } else {
                sb.appendLine("   ($num) ${item.lineItemName}: ${item.statutoryAuthority}")
            }
        }
        return sb.toString().trimEnd()
    }

    private fun formatCurrency(amount: Double): String {
        val intVal = amount.toInt()
        return "Rs. $intVal"
    }

    private fun buildFullBody(
        req: RedressalRequest,
        name: String,
        sNum: String,
        cda: String,
        dateStr: String,
        tableText: String,
        citationsText: String,
        totalDue: Double,
    ): String {
        val remarks = req.prayerRemarks?.let { "\n   $it\n" } ?: ""
        return """
            CONFIDENTIAL & OFFICIAL MILITARY CORRESPONDENCE
            -----------------------------------------------------------------------
            To,
            $PCDA_RECIPIENT

            ATTENTION: ${req.ledgerSection}
            SUBJECT  : FORMAL REPRESENTATION REGARDING DISCREPANCY IN RUNNING LEDGER ACCOUNT (IRLA)
            CDA A/C  : $cda
            OFFICER  : ${req.rank} $name ($sNum)
            DATE     : $dateStr
            -----------------------------------------------------------------------

            Sir / Madam,

            1. I have the honour to draw your kind attention to my Individual Running Ledger Account (IRLA)
               under CDA Account No. $cda for the period of ${req.disputeMonth}. Upon audit synthesis against
               the canonical orders of the 7th Central Pay Commission and Ministry of Defence regulations,
               the following statutory dues remain omitted / under-credited to my account:

            $tableText

            2. STATUTORY AUTHORITY & REFERENCES:
            $citationsText

            3. PRAYER:$remarks
               In light of the documentary references cited above, it is respectfully requested that
               the statutory dues of ${formatCurrency(totalDue)} be credited to my IRLA at the earliest convenience, and an amended
               Statement of Account (SOA) be issued.

            Thanking you,

            Yours faithfully,

            ($name)
            ${req.rank}, Indian Army
            -----------------------------------------------------------------------
            """.trimIndent()
    }
}
