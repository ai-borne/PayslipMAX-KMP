package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.ParsedPayslip

class UnexpectedDebitAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>
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

        val recoveryType = when {
            debitRecovery > 0.0 && ticketRecovery > 0.0 -> "Debit and LTC Ticket Recoveries"
            debitRecovery > 0.0 -> "Retroactive Debit Recovery"
            else -> "LTC Ticket Recovery"
        }

        anomalies.add(
            Anomaly(
                type = "DEBIT_RECOVERY",
                field = "recoveryOfDebits",
                amount = totalRecovery,
                month = current.dateStr,
                description = "Unexpected deduction of ₹${totalRecovery.toInt()} ($recoveryType) consumed ${ratio.toString().take(4)}% of your gross monthly pay."
            )
        )

        return anomalies
    }
}
