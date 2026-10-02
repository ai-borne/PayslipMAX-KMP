package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip

class UnexpectedDebitAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()
        val debitRecovery = current.deductions.recoveryOfDebits
        val ticketRecovery = current.deductions.ticketRecovery

        val totalRecovery = debitRecovery + ticketRecovery
        if (totalRecovery <= 0.0) {
            return anomalies
        }

        val gross = current.summary.grossPay
        val ratio = if (gross > 0.0) (totalRecovery / gross) * 100.0 else 0.0

        val recoveryType =
            when {
                debitRecovery > 0.0 && ticketRecovery > 0.0 -> "recovery of earlier debits and LTC tickets"
                debitRecovery > 0.0 -> "recovery of an earlier debit"
                else -> "recovery of LTC ticket cost"
            }

        anomalies.add(
            Anomaly(
                type = "DEBIT_RECOVERY",
                field = "recoveryOfDebits",
                amount = totalRecovery,
                month = current.dateStr,
                description = "Unexpected deduction of ${PayAuditWording.rupees(totalRecovery)} ($recoveryType), which is ${ratio.toString().take(4)}% of your gross monthly pay.",
            ),
        )

        return anomalies
    }
}
