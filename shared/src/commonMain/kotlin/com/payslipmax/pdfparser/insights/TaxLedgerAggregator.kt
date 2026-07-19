package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlinx.serialization.Serializable

@Serializable
data class FyTaxLedgerSummary(
    val financialYear: String,
    val assessmentYear: String,
    val parsedMonthCount: Int,
    val ytdGross: Double,
    val ytdTaxDeducted: Double,
    val ytdDsop: Double,
    val ytdAgif: Double,
    val ytdFieldAllowance: Double,
    val ytdHra: Double,
    val ytdBasicPay: Double,
    val ytdDa: Double,
    val projectedAnnualGross: Double,
    val projectedAnnualTaxDeducted: Double,
    val projectedAnnualDsop: Double,
    val projectedAnnualAgif: Double,
    val projectedAnnualFieldAllowance: Double,
    val projectedAnnualHra: Double,
    val projectedAnnualBasicPay: Double,
    val projectedAnnualDa: Double,
    val missingMonthNums: List<Int>,
    val latestBasicPay: Double,
    val latestDa: Double,
)

object TaxLedgerAggregator {
    fun formatIndianCurrency(amount: Double): String {
        val rounded = kotlin.math.round(amount).toLong()
        val isNegative = rounded < 0
        val absStr = kotlin.math.abs(rounded).toString()
        if (absStr.length <= 3) {
            return (if (isNegative) "-" else "") + absStr
        }
        val lastThree = absStr.takeLast(3)
        val remaining = absStr.dropLast(3)
        val formattedRemaining = remaining.reversed().chunked(2).joinToString(",").reversed()
        val result = "$formattedRemaining,$lastThree"
        return if (isNegative) "-$result" else result
    }

    fun computeFinancialYear(
        year: Int,
        monthNum: Int,
    ): String {
        val startYear = if (monthNum >= 4) year else year - 1
        val endYearShort = (startYear + 1) % 100
        val endYearStr = if (endYearShort < 10) "0$endYearShort" else "$endYearShort"
        return "$startYear-$endYearStr"
    }

    fun computeAssessmentYear(financialYear: String): String {
        val parts = financialYear.split("-")
        if (parts.size != 2) return financialYear
        val startYear = parts[0].toIntOrNull() ?: return financialYear
        val ayStart = startYear + 1
        val ayEndShort = (ayStart + 1) % 100
        val ayEndStr = if (ayEndShort < 10) "0$ayEndShort" else "$ayEndShort"
        return "$ayStart-$ayEndStr"
    }

    fun aggregateFy(
        payslips: List<ParsedPayslip>,
        targetFy: String? = null,
    ): FyTaxLedgerSummary {
        if (payslips.isEmpty()) {
            return createEmptySummary(targetFy ?: "2024-25")
        }

        val activeFy = targetFy ?: computeFinancialYear(payslips.last().year, payslips.last().monthNum)
        val fyPayslips =
            payslips.filter { computeFinancialYear(it.year, it.monthNum) == activeFy }
                .sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum })

        if (fyPayslips.isEmpty()) {
            return createEmptySummary(activeFy)
        }

        return buildFySummary(activeFy, fyPayslips)
    }

    private fun buildFySummary(
        activeFy: String,
        fyPayslips: List<ParsedPayslip>,
    ): FyTaxLedgerSummary {
        val count = fyPayslips.size
        val multiplier = if (count > 0) 12.0 / count else 1.0

        val ytdGross = fyPayslips.sumOf { it.summary.grossPay }
        val ytdTax = fyPayslips.sumOf { it.deductions.incomeTax }
        val ytdDsop = fyPayslips.sumOf { it.deductions.dsopSubscription }
        val ytdAgif = fyPayslips.sumOf { it.deductions.agif }
        val ytdField = fyPayslips.sumOf { it.earnings.fieldAllowance }
        val ytdHra = fyPayslips.sumOf { it.earnings.houseRentAllowance }
        val ytdBasic = fyPayslips.sumOf { it.earnings.basicPay }
        val ytdDa = fyPayslips.sumOf { it.earnings.dearnessAllowance }

        val presentMonths = fyPayslips.map { it.monthNum }.toSet()
        val missingMonths = (1..12).filter { m -> !presentMonths.contains(m) }

        val latest = fyPayslips.last()

        return FyTaxLedgerSummary(
            financialYear = activeFy,
            assessmentYear = computeAssessmentYear(activeFy),
            parsedMonthCount = count,
            ytdGross = ytdGross,
            ytdTaxDeducted = ytdTax,
            ytdDsop = ytdDsop,
            ytdAgif = ytdAgif,
            ytdFieldAllowance = ytdField,
            ytdHra = ytdHra,
            ytdBasicPay = ytdBasic,
            ytdDa = ytdDa,
            projectedAnnualGross = ytdGross * multiplier,
            projectedAnnualTaxDeducted = ytdTax * multiplier,
            projectedAnnualDsop = ytdDsop * multiplier,
            projectedAnnualAgif = ytdAgif * multiplier,
            projectedAnnualFieldAllowance = ytdField * multiplier,
            projectedAnnualHra = ytdHra * multiplier,
            projectedAnnualBasicPay = ytdBasic * multiplier,
            projectedAnnualDa = ytdDa * multiplier,
            missingMonthNums = missingMonths,
            latestBasicPay = latest.earnings.basicPay,
            latestDa = latest.earnings.dearnessAllowance,
        )
    }

    private fun createEmptySummary(fy: String): FyTaxLedgerSummary {
        return FyTaxLedgerSummary(
            financialYear = fy,
            assessmentYear = computeAssessmentYear(fy),
            parsedMonthCount = 0,
            ytdGross = 0.0, ytdTaxDeducted = 0.0, ytdDsop = 0.0, ytdAgif = 0.0,
            ytdFieldAllowance = 0.0, ytdHra = 0.0, ytdBasicPay = 0.0, ytdDa = 0.0,
            projectedAnnualGross = 0.0, projectedAnnualTaxDeducted = 0.0, projectedAnnualDsop = 0.0,
            projectedAnnualAgif = 0.0, projectedAnnualFieldAllowance = 0.0, projectedAnnualHra = 0.0,
            projectedAnnualBasicPay = 0.0, projectedAnnualDa = 0.0,
            missingMonthNums = (1..12).toList(), latestBasicPay = 0.0, latestDa = 0.0,
        )
    }
}
