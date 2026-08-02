package com.payslipmax.pdfparser.tax

import kotlinx.serialization.Serializable

@Serializable
data class ResolvedTaxYear(
    val financialYear: String,
    val assessmentYear: String,
)

object TaxYearResolver {
    fun resolve(
        monthNum: Int,
        year: Int,
    ): ResolvedTaxYear {
        val startYear = if (monthNum >= 4) year else year - 1
        val fyEndShort = (startYear + 1) % 100
        val fyEndStr = if (fyEndShort < 10) "0$fyEndShort" else fyEndShort.toString()
        val fy = "$startYear-$fyEndStr"

        val ayStart = startYear + 1
        val ayEndShort = (ayStart + 1) % 100
        val ayEndStr = if (ayEndShort < 10) "0$ayEndShort" else ayEndShort.toString()
        val ay = "$ayStart-$ayEndStr"

        return ResolvedTaxYear(financialYear = fy, assessmentYear = ay)
    }
}
