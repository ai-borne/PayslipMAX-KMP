package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip

interface RuleAuditor {
    fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly>
}
