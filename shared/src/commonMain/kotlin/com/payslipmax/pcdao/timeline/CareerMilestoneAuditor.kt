package com.payslipmax.pcdao.timeline

import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.math.roundToInt

class CareerMilestoneAuditor {
    fun auditMilestones(payslips: List<ParsedPayslip>): List<CareerMilestone> {
        if (payslips.size < 2) return emptyList()
        val chronological = VaultMonthGroupMapper.sortOldestFirst(payslips)
        val milestones = mutableListOf<CareerMilestone>()

        var establishedIncrementMonth: Int? = null

        for (i in 1 until chronological.size) {
            val prev = chronological[i - 1]
            val curr = chronological[i]

            val incrementMilestone = auditIncrement(prev, curr)
            if (incrementMilestone != null) {
                milestones.add(incrementMilestone)
                if (incrementMilestone.type == MilestoneType.ANNUAL_INCREMENT_VERIFIED) {
                    establishedIncrementMonth = curr.monthNum
                }
            } else if (establishedIncrementMonth != null && curr.monthNum == establishedIncrementMonth) {
                val missingAlert = checkMissingIncrement(prev, curr, establishedIncrementMonth)
                if (missingAlert != null) milestones.add(missingAlert)
            }

            val daMilestone = auditDearnessAllowance(prev, curr)
            if (daMilestone != null) milestones.add(daMilestone)

            val arrearsSpike = auditArrearsSpike(prev, curr)
            if (arrearsSpike != null) milestones.add(arrearsSpike)
        }

        return milestones.reversed()
    }

    private fun auditIncrement(
        prev: ParsedPayslip,
        curr: ParsedPayslip,
    ): CareerMilestone? {
        val prevBasic = prev.earnings.basicPay
        val currBasic = curr.earnings.basicPay
        val diff = currBasic - prevBasic

        if (diff > 500.0 && (curr.monthNum == 1 || curr.monthNum == 7)) {
            val formattedDiff = diff.roundToInt()
            return CareerMilestone(
                type = MilestoneType.ANNUAL_INCREMENT_VERIFIED,
                dateStr = curr.dateStr,
                monthName = curr.monthName,
                year = curr.year,
                title = "Annual Increment Verified (${curr.monthName} ${curr.year})",
                description = "Basic Pay incremented from ₹${prevBasic.toLong()} to ₹${currBasic.toLong()} (+₹$formattedDiff).",
                monetaryImpact = diff,
                statutoryAuthority = "Army Officers Pay Rules 2017, Rule 10 (Annual Increment)",
                isAlert = false,
            )
        }
        return null
    }

    private fun checkMissingIncrement(
        prev: ParsedPayslip,
        curr: ParsedPayslip,
        expectedMonth: Int,
    ): CareerMilestone? {
        if (curr.earnings.basicPay <= prev.earnings.basicPay && curr.monthNum == expectedMonth) {
            return CareerMilestone(
                type = MilestoneType.ANNUAL_INCREMENT_MISSING,
                dateStr = curr.dateStr,
                monthName = curr.monthName,
                year = curr.year,
                title = "Statutory Increment Not Reflected (${curr.monthName} ${curr.year})",
                description = "Basic Pay remained flat at ₹${curr.earnings.basicPay.toLong()} during scheduled increment cycle.",
                monetaryImpact = 0.0,
                statutoryAuthority = "Army Officers Pay Rules 2017, Rule 10",
                isAlert = true,
            )
        }
        return null
    }

    private fun auditDearnessAllowance(
        prev: ParsedPayslip,
        curr: ParsedPayslip,
    ): CareerMilestone? {
        val prevDa = prev.earnings.dearnessAllowance
        val currDa = curr.earnings.dearnessAllowance
        val diff = currDa - prevDa

        if (diff > 1000.0 && curr.earnings.basicPay == prev.earnings.basicPay) {
            val formattedDiff = diff.roundToInt()
            return CareerMilestone(
                type = MilestoneType.DA_REVISION_CREDITED,
                dateStr = curr.dateStr,
                monthName = curr.monthName,
                year = curr.year,
                title = "DA Revision Adjustment (${curr.monthName} ${curr.year})",
                description = "Dearness Allowance increased from ₹${prevDa.toLong()} to ₹${currDa.toLong()} (+₹$formattedDiff/mo).",
                monetaryImpact = diff,
                statutoryAuthority = "MoD / DoE Central DA Orders",
                isAlert = false,
            )
        }
        return null
    }

    private fun auditArrearsSpike(
        prev: ParsedPayslip,
        curr: ParsedPayslip,
    ): CareerMilestone? {
        val prevGross = prev.summary.grossPay
        val currGross = curr.summary.grossPay
        val grossSpike = currGross - prevGross

        if (grossSpike > 15000.0 && curr.earnings.basicPay == prev.earnings.basicPay) {
            val formattedSpike = grossSpike.roundToInt()
            return CareerMilestone(
                type = MilestoneType.DA_ARREARS_SPIKE,
                dateStr = curr.dateStr,
                monthName = curr.monthName,
                year = curr.year,
                title = "Retroactive Arrears Credit (${curr.monthName} ${curr.year})",
                description = "Gross pay spiked by +₹$formattedSpike over previous month, reflecting retroactive arrears credit.",
                monetaryImpact = grossSpike,
                statutoryAuthority = "PCDA(O) Running Ledger Account (RLA) Arrears",
                isAlert = false,
            )
        }
        return null
    }
}
