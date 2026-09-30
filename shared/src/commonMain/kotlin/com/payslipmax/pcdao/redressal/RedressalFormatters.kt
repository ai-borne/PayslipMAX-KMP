package com.payslipmax.pcdao.redressal

internal object RedressalFormatters {
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
            sb.appendLine(
                "| ${item.serialNo} | ${item.lineItemName} | ${formatCurrency(item.entitledAmount)} | " +
                    "${formatCurrency(item.creditedAmount)} | ${formatCurrency(item.netDue)} |",
            )
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

    fun formatCurrency(amount: Double): String {
        val intVal = amount.toInt()
        return "Rs. $intVal"
    }

    fun buildFullBody(
        req: RedressalRequest,
        name: String,
        sNum: String,
        cda: String,
        dateStr: String,
        tableText: String,
        citationsText: String,
        totalDue: Double,
        recipient: String,
    ): String {
        val remarks = req.prayerRemarks?.let { "\n   $it\n" } ?: ""
        val dueStr = formatCurrency(totalDue)
        val header = buildLetterHeader(recipient, req.ledgerSection, cda, req.rank, name, sNum, dateStr)
        val signOff = buildLetterSignOff(name, req.rank, cda)
        return """
            $header

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
               the statutory dues of $dueStr be credited to my IRLA at the earliest convenience, and an amended
               Statement of Account (SOA) be issued.

            Thanking you,

            $signOff
            """.trimIndent()
    }

    private fun buildLetterHeader(
        recipient: String,
        section: String,
        cda: String,
        rank: String,
        name: String,
        sNum: String,
        dateStr: String,
    ): String =
        """
        CONFIDENTIAL & OFFICIAL MILITARY CORRESPONDENCE
        -----------------------------------------------------------------------
        To,
        $recipient

        ATTENTION: $section
        SUBJECT  : FORMAL REPRESENTATION REGARDING DISCREPANCY IN RUNNING LEDGER ACCOUNT (IRLA)
        CDA A/C  : $cda
        OFFICER  : $rank $name ($sNum)
        DATE     : $dateStr
        -----------------------------------------------------------------------
        """.trimIndent()

    private fun buildLetterSignOff(
        name: String,
        rank: String,
        cda: String,
    ): String =
        """
        Yours faithfully,

        ($name)
        $rank, Indian Army
        CDA A/C: $cda
        -----------------------------------------------------------------------
        """.trimIndent()
}
