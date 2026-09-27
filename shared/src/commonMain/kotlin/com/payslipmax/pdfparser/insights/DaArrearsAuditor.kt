package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Checks DA arrears paid in the month a DA rise first appears against the structural rule:
 * rise % x (Basic + MSP) x months since the rise's effective date (1 Jan or 1 Jul).
 * The DA rate is read from the payslips themselves, so no DA rate table needs maintaining.
 */
class DaArrearsAuditor : RuleAuditor {
    override fun audit(
        current: ParsedPayslip,
        previous: ParsedPayslip?,
        history: List<ParsedPayslip>,
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()
        val arrearsDa = current.earnings.arrearsDa
        val arrearsTptaDa = current.earnings.arrearsTptaDa

        if (arrearsDa <= 0.0 && arrearsTptaDa <= 0.0) {
            return anomalies
        }
        if (previous == null || isBeforeSeventhCpcPay(current)) {
            return anomalies
        }

        val payBase = current.earnings.basicPay + current.earnings.militaryServicePay
        val prevPayBase = previous.earnings.basicPay + previous.earnings.militaryServicePay
        if (current.earnings.basicPay <= 0.0 || prevPayBase <= 0.0) {
            return anomalies
        }

        val currentRatePercent = wholePercent(current.earnings.dearnessAllowance / payBase)
        val prevRatePercent = wholePercent(previous.earnings.dearnessAllowance / prevPayBase)
        val months = monthsSinceEffectiveDate(current.monthNum)
        if (currentRatePercent <= prevRatePercent || months <= 0) {
            return anomalies
        }

        val rateDiff = (currentRatePercent - prevRatePercent) / 100.0
        val expectedDa = payBase * rateDiff * months
        val tpta = current.earnings.transportAllowance
        val tptaDaIsSeparate = current.earnings.transportAllowanceDa > 0.0 || arrearsTptaDa > 0.0

        if (tptaDaIsSeparate) {
            checkArrears(anomalies, arrearsDa, expectedDa, current.dateStr, "Dearness Allowance (DA)")
            if (tpta > 0.0 && arrearsTptaDa > 0.0) {
                val expectedTptaDa = tpta * rateDiff * months
                checkArrears(anomalies, arrearsTptaDa, expectedTptaDa, current.dateStr, "Transport Allowance DA (TPTA DA)")
            }
        } else {
            // Older payslips print TPTA inclusive of its DA and fold TPTA-DA arrears into the DA arrears line.
            val tptaBase = tpta / (1.0 + currentRatePercent / 100.0)
            val expectedWithTptaDa = expectedDa + tptaBase * rateDiff * months
            val bestMatch = if (abs(arrearsDa - expectedWithTptaDa) < abs(arrearsDa - expectedDa)) expectedWithTptaDa else expectedDa
            checkArrears(anomalies, arrearsDa, bestMatch, current.dateStr, "Dearness Allowance (DA)")
        }

        return anomalies
    }

    private fun wholePercent(rate: Double): Int = (rate * 100.0).roundToInt()

    /** Months from the rise's effective date (1 Jan for Jan-Jun payslips, 1 Jul for Jul-Dec) up to the month before this payslip. */
    private fun monthsSinceEffectiveDate(monthNum: Int): Int = if (monthNum <= 6) monthNum - 1 else monthNum - 7

    /** 6th CPC-era DA (100%+ on a different pay base) does not follow this arrears math. */
    private fun isBeforeSeventhCpcPay(payslip: ParsedPayslip): Boolean =
        payslip.year * 12 + payslip.monthNum < SEVENTH_CPC_FIRST_YEAR * 12 + SEVENTH_CPC_FIRST_MONTH

    private fun checkArrears(
        anomalies: MutableList<Anomaly>,
        actual: Double,
        expected: Double,
        month: String,
        label: String,
    ) {
        if (actual <= 0.0) return
        val diff = abs(actual - expected)
        if (diff < 100.0) {
            anomalies.add(
                Anomaly(
                    type = "ARREARS_AUDIT",
                    field = "arrearsDa",
                    amount = actual,
                    month = month,
                    description = "Verified: Your $label arrears of ₹${actual.toInt()} match the expected calculation exactly.",
                ),
            )
        } else if (actual < expected) {
            anomalies.add(
                Anomaly(
                    type = "SALARY_LOSS",
                    field = "arrearsDa",
                    amount = expected - actual,
                    month = month,
                    description = "Underpaid/Mismatched: Your $label arrears of ₹${actual.toInt()} do not match the expected calculation of ₹${expected.toInt()}.",
                ),
            )
        }
    }

    private companion object {
        // First month PCDA(O) payslips carry 7th CPC pay (per the corpus, Jun 2017).
        const val SEVENTH_CPC_FIRST_YEAR = 2017
        const val SEVENTH_CPC_FIRST_MONTH = 6
    }
}
