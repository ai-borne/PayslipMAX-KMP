package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip

object RedactionSanitizer {
    /**
     * Redacts all Personally Identifiable Information (PII) from a ParsedPayslip.
     * Replaces Officer name, Account Number, PAN, and filepath with generic indicators.
     */
    fun redact(payslip: ParsedPayslip): ParsedPayslip {
        return payslip.copy(
            file = "payslip.pdf",
            officer =
                Officer(
                    name = "[OFFICER_NAME_REDACTED]",
                    accountNo = "[ACCOUNT_NO_REDACTED]",
                    pan = "[PAN_REDACTED]",
                ),
        )
    }

    /**
     * Redacts PII from a list of payslips.
     */
    fun redactList(payslips: List<ParsedPayslip>): List<ParsedPayslip> {
        return payslips.map { redact(it) }
    }
}
