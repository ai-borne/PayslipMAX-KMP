package com.payslipmax.pcdao.timeline

import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciler
import com.payslipmax.pdfparser.domain.ParsedPayslip
import kotlin.math.abs
import kotlin.math.round

class CumulativeLedgerRollupEngine(
    private val reconciler: ShadowLedgerReconciler = ShadowLedgerReconciler(),
) {
    fun calculateRollup(
        payslips: List<ParsedPayslip>,
        context: ActiveSituationalContext,
    ): CumulativeArrearsRollup {
        if (payslips.isEmpty()) {
            return CumulativeArrearsRollup(
                totalUnderpaidArrears = 0.0,
                totalRecoveryHazard = 0.0,
                auditedMonthCount = 0,
                startMonthDateStr = "",
                endMonthDateStr = "",
            )
        }

        val chronological = VaultMonthGroupMapper.sortOldestFirst(payslips)
        var totalUnderpaid = 0.0
        var totalHazard = 0.0
        val breakdowns = mutableListOf<MonthArrearsBreakdown>()
        var primaryClaim = ""

        for (slip in chronological) {
            val result = reconciler.reconcile(slip, context)

            for (d in result.discrepancies) {
                if (d.type == DiscrepancyType.UNDERPAYMENT && d.monthlyImpact > 0.0) {
                    totalUnderpaid += d.monthlyImpact
                    if (primaryClaim.isEmpty()) primaryClaim = d.title
                    breakdowns.add(
                        MonthArrearsBreakdown(
                            dateStr = slip.dateStr,
                            monthName = slip.monthName,
                            year = slip.year,
                            basicPay = slip.earnings.basicPay,
                            allowanceName = d.title,
                            entitledAmount = d.entitledAmount,
                            creditedAmount = d.drawnAmount,
                            arrearsDue = d.monthlyImpact,
                        ),
                    )
                } else if (d.type == DiscrepancyType.RECOVERY_HAZARD) {
                    totalHazard += abs(d.monthlyImpact)
                }
            }
        }

        val roundedUnderpaid = round(totalUnderpaid * 100.0) / 100.0
        val roundedHazard = round(totalHazard * 100.0) / 100.0
        val startMonth = chronological.first().dateStr
        val endMonth = chronological.last().dateStr

        val summary =
            if (roundedUnderpaid > 0.0) {
                "Cumulative back-dues of ₹${roundedUnderpaid.toLong()} across ${chronological.size} uploaded payslips ($startMonth to $endMonth)."
            } else {
                "Clean ledger audit across all ${chronological.size} payslips. No cumulative back-dues detected."
            }

        return CumulativeArrearsRollup(
            totalUnderpaidArrears = roundedUnderpaid,
            totalRecoveryHazard = roundedHazard,
            auditedMonthCount = chronological.size,
            startMonthDateStr = startMonth,
            endMonthDateStr = endMonth,
            monthlyBreakdowns = breakdowns,
            primaryClaimTitle = primaryClaim,
            summaryText = summary,
        )
    }
}
