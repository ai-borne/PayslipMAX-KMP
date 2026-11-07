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
        if (currentRatePercent <= prevRatePercent || DaArrearsWindow.minimumMonths(current.monthNum) == null) {
            return anomalies
        }

        val rateDiff = (currentRatePercent - prevRatePercent) / 100.0
        val daPerMonth = payBase * rateDiff
        val tpta = current.earnings.transportAllowance
        val tptaDaIsSeparate = current.earnings.transportAllowanceDa > 0.0 || arrearsTptaDa > 0.0

        if (tptaDaIsSeparate) {
            val expectedDa = expectedArrears(current.monthNum, arrearsDa, daPerMonth)
            checkArrears(anomalies, arrearsDa, expectedDa, current.dateStr, "Dearness Allowance (DA)", ARREARS_DA_FIELD)
            if (tpta > 0.0 && arrearsTptaDa > 0.0) {
                val expectedTptaDa = expectedArrears(current.monthNum, arrearsTptaDa, tpta * rateDiff)
                checkArrears(anomalies, arrearsTptaDa, expectedTptaDa, current.dateStr, "Dearness Allowance on your Transport Allowance", ARREARS_TPTA_DA_FIELD)
            }
        } else {
            // Older payslips print TPTA inclusive of its DA and fold TPTA-DA arrears into the DA arrears line.
            val tptaBase = tpta / (1.0 + currentRatePercent / 100.0)
            val withoutTptaDa = expectedArrears(current.monthNum, arrearsDa, daPerMonth)
            val withTptaDa = expectedArrears(current.monthNum, arrearsDa, daPerMonth + tptaBase * rateDiff)
            val bestMatch = if (abs(arrearsDa - withTptaDa) < abs(arrearsDa - withoutTptaDa)) withTptaDa else withoutTptaDa
            checkArrears(anomalies, arrearsDa, bestMatch, current.dateStr, "Dearness Allowance (DA)", ARREARS_DA_FIELD)
        }

        return anomalies
    }

    private fun wholePercent(rate: Double): Int = (rate * 100.0).roundToInt()

    /**
     * The arrears owed for [perMonth]: the amount of whichever candidate month count ([DaArrearsWindow]) the payment matches, so
     * arrears paid in any later half-year are verified. A payment matching none is measured against the fewest months it can
     * cover, so a shortfall never claims more than the minimum owed.
     */
    private fun expectedArrears(
        monthNum: Int,
        actual: Double,
        perMonth: Double,
    ): Double {
        val minimum = perMonth * (DaArrearsWindow.minimumMonths(monthNum) ?: return 0.0)
        val closest = perMonth * (DaArrearsWindow.closestMonths(monthNum, actual, perMonth) ?: return minimum)
        return if (abs(actual - closest) < TOLERANCE_RUPEES) closest else minimum
    }

    /** 6th CPC-era DA (100%+ on a different pay base) does not follow this arrears math. */
    private fun isBeforeSeventhCpcPay(payslip: ParsedPayslip): Boolean =
        payslip.year * 12 + payslip.monthNum < SEVENTH_CPC_FIRST_YEAR * 12 + SEVENTH_CPC_FIRST_MONTH

    private fun checkArrears(
        anomalies: MutableList<Anomaly>,
        actual: Double,
        expected: Double,
        month: String,
        label: String,
        field: String,
    ) {
        if (actual <= 0.0) return
        val diff = abs(actual - expected)
        if (diff < TOLERANCE_RUPEES) {
            anomalies.add(
                Anomaly(
                    type = "ARREARS_AUDIT",
                    field = field,
                    amount = actual,
                    month = month,
                    description = "Verified: your $label arrears of ${PayAuditWording.rupees(actual)} match the amount worked out from your DA rise exactly.",
                    expected = expected,
                    actual = actual,
                ),
            )
        } else if (actual < expected) {
            anomalies.add(
                Anomaly(
                    type = "SALARY_LOSS",
                    field = field,
                    amount = expected - actual,
                    month = month,
                    description =
                        "Your $label arrears are ${PayAuditWording.rupees(actual)}, which is ${PayAuditWording.rupees(expected - actual)} less than the " +
                            "${PayAuditWording.rupees(expected)} worked out from your DA rise. If part of it is paid on a later payslip, this will clear.",
                    expected = expected,
                    actual = actual,
                ),
            )
        }
    }

    private companion object {
        const val TOLERANCE_RUPEES = 100.0
        const val ARREARS_DA_FIELD = "arrearsDa"
        const val ARREARS_TPTA_DA_FIELD = "arrearsTptaDa"

        // First month PCDA(O) payslips carry 7th CPC pay (per the corpus, Jun 2017).
        const val SEVENTH_CPC_FIRST_YEAR = 2017
        const val SEVENTH_CPC_FIRST_MONTH = 6
    }
}
