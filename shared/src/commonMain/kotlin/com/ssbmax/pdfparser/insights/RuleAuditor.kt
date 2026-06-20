package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.ParsedPayslip

interface RuleAuditor {
    fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>
    ): List<Anomaly>
}
