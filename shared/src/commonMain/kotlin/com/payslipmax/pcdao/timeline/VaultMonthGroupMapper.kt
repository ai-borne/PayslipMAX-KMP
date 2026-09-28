package com.payslipmax.pcdao.timeline

import com.payslipmax.pdfparser.domain.ParsedPayslip

object VaultMonthGroupMapper {
    fun sortNewestFirst(payslips: List<ParsedPayslip>): List<ParsedPayslip> =
        payslips.sortedWith(
            compareByDescending<ParsedPayslip> { it.year }
                .thenByDescending { it.monthNum },
        )

    fun sortOldestFirst(payslips: List<ParsedPayslip>): List<ParsedPayslip> =
        payslips.sortedWith(
            compareBy<ParsedPayslip> { it.year }
                .thenBy { it.monthNum },
        )

    fun getFinancialYearLabel(
        year: Int,
        monthNum: Int,
    ): String {
        val startYear = if (monthNum >= 4) year else year - 1
        val endYearShort = (startYear + 1) % 100
        val endYearFormatted = if (endYearShort < 10) "0$endYearShort" else "$endYearShort"
        return "FY $startYear-$endYearFormatted"
    }

    fun groupByFinancialYear(payslips: List<ParsedPayslip>): Map<String, List<ParsedPayslip>> {
        val sorted = sortNewestFirst(payslips)
        val linkedMap = LinkedHashMap<String, MutableList<ParsedPayslip>>()
        for (slip in sorted) {
            val fy = getFinancialYearLabel(slip.year, slip.monthNum)
            linkedMap.getOrPut(fy) { mutableListOf() }.add(slip)
        }
        return linkedMap
    }
}
