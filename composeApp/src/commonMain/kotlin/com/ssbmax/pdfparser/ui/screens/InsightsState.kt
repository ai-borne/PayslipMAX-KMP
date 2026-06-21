package com.ssbmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.insights.DeterministicIntelligenceEngine
import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.insights.OptimizationResult
import com.ssbmax.pdfparser.insights.WealthOptimizationEngine
import com.ssbmax.pdfparser.parser.PayslipPatternConfig

data class InsightsState(
    val currentRecord: LedgerRecordEntity,
    val previousRecord: LedgerRecordEntity?,
    val historySorted: List<LedgerRecordEntity>,
    val engineResult: EngineResult,
    val scoreDelta: Int?,
    val optimizationResult: OptimizationResult,
    val momChanges: List<MoMChange>,
    val previousMonthLabel: String?,
)

fun previousMonthLabel(previousRecord: LedgerRecordEntity?): String? =
    previousRecord?.let { PayslipPatternConfig.monthNames.getOrNull(it.monthNum) }

@Composable
fun rememberInsightsState(
    selected: ParsedPayslip,
    ledgerRecords: List<LedgerRecordEntity>,
): InsightsState {
    val currentRecord = remember(ledgerRecords, selected) { findCurrentRecord(selected, ledgerRecords) }
    val previousRecord = remember(ledgerRecords, currentRecord) { findPreviousRecord(currentRecord, ledgerRecords) }
    val historySorted =
        remember(ledgerRecords) {
            ledgerRecords.sortedWith(compareBy<LedgerRecordEntity> { it.year }.thenBy { it.monthNum })
        }
    val engineResult =
        remember(currentRecord, previousRecord, historySorted) {
            DeterministicIntelligenceEngine.analyze(currentRecord, previousRecord, historySorted)
        }
    val previousScore = remember(previousRecord, ledgerRecords) { calculatePreviousScore(previousRecord, ledgerRecords) }
    val scoreDelta =
        remember(engineResult.healthScore, previousScore) {
            previousScore?.let { engineResult.healthScore - it }
        }
    val optimizationResult = remember(selected) { WealthOptimizationEngine.analyze(selected) }
    val momChanges = remember(currentRecord, previousRecord) { calculateMomChanges(currentRecord, previousRecord) }
    val monthLabel = remember(previousRecord) { previousMonthLabel(previousRecord) }
    return remember(
        currentRecord,
        previousRecord,
        historySorted,
        engineResult,
        scoreDelta,
        optimizationResult,
        momChanges,
        monthLabel,
    ) {
        InsightsState(
            currentRecord = currentRecord,
            previousRecord = previousRecord,
            historySorted = historySorted,
            engineResult = engineResult,
            scoreDelta = scoreDelta,
            optimizationResult = optimizationResult,
            momChanges = momChanges,
            previousMonthLabel = monthLabel,
        )
    }
}

private fun findCurrentRecord(
    selected: ParsedPayslip,
    ledgerRecords: List<LedgerRecordEntity>,
): LedgerRecordEntity {
    return ledgerRecords.find { it.dateStr == selected.dateStr } ?: LedgerRecordEntity(
        dateStr = selected.dateStr, year = selected.year, monthNum = selected.monthNum,
        basicPay = selected.earnings.basicPay, dearnessAllowance = selected.earnings.dearnessAllowance,
        militaryServicePay = selected.earnings.militaryServicePay, transportAllowance = selected.earnings.transportAllowance,
        transportAllowanceDa = selected.earnings.transportAllowanceDa, houseRentAllowance = selected.earnings.houseRentAllowance,
        grossPay = selected.summary.grossPay, dsopSubscription = selected.deductions.dsopSubscription,
        incomeTax = selected.deductions.incomeTax, netPay = selected.summary.netRemittance,
    )
}

private fun findPreviousRecord(
    currentRecord: LedgerRecordEntity,
    ledgerRecords: List<LedgerRecordEntity>,
): LedgerRecordEntity? {
    return ledgerRecords.find {
        val isPrevYear = it.year == currentRecord.year && it.monthNum == currentRecord.monthNum - 1
        val isDecToJan = it.year == currentRecord.year - 1 && it.monthNum == 12 && currentRecord.monthNum == 1
        isPrevYear || isDecToJan
    }
}

private fun calculatePreviousScore(
    previousRecord: LedgerRecordEntity?,
    ledgerRecords: List<LedgerRecordEntity>,
): Int? {
    return previousRecord?.let { prev ->
        val prevPrev =
            ledgerRecords.find {
                val isPrevYear = it.year == prev.year && it.monthNum == prev.monthNum - 1
                val isDecToJan = it.year == prev.year - 1 && it.monthNum == 12 && prev.monthNum == 1
                isPrevYear || isDecToJan
            }
        val prevHistory = ledgerRecords.filter { it.year < prev.year || (it.year == prev.year && it.monthNum <= prev.monthNum) }
        DeterministicIntelligenceEngine.analyze(prev, prevPrev, prevHistory).healthScore
    }
}
