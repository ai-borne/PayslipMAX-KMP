package com.payslipmax.pdfparser.debugseed

import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import kotlinx.coroutines.flow.first

sealed interface SeedResult {
    data class Seeded(val months: List<String>) : SeedResult

    /** Some target month already holds a payslip, ledger row, insight or draft: nothing was written. */
    data class Collision(val months: List<String>) : SeedResult

    data class MissingPrerequisite(val step: SeedStep) : SeedResult
}

/**
 * Writes the synthetic payslips of a [SeedStep] the way a real import does (stored payslip, then the audit
 * through [FinancialIntelligenceRepository], so ledger rows, insights, drafts and the Phase 7 re-audit all
 * run), and removes exactly what it wrote. It never overwrites: a target month that already has *anything*
 * stored refuses the step, which is also what makes deleting by month safe.
 */
class DebugSeeder(
    private val dao: PayslipDao,
    private val intelligence: FinancialIntelligenceRepository,
) {
    suspend fun apply(step: SeedStep): SeedResult {
        step.requires?.let { base ->
            val have = seededMonths()
            if (!SyntheticSeedPayslips.payslips(base).all { it.dateStr in have }) return SeedResult.MissingPrerequisite(base)
        }
        val slips = SyntheticSeedPayslips.payslips(step)
        val taken = occupiedMonths()
        val clashes = slips.map { it.dateStr }.filter { it in taken }
        if (clashes.isNotEmpty()) return SeedResult.Collision(clashes)

        slips.forEach {
            dao.insertPayslip(it.toEncryptedEntity())
            intelligence.processPayslipAndRunAnalysis(it)
        }
        return SeedResult.Seeded(slips.map { it.dateStr })
    }

    /** Deletes every seeded month with its ledger row, insights and drafts; returns how many months. */
    suspend fun remove(): Int {
        val months = seededMonths()
        months.forEach {
            dao.deletePayslip(it)
            dao.deleteCorrection(it)
            dao.deletePayslipPdf(it)
            dao.deleteLedgerRecord(it)
            dao.deleteFinancialInsightsByMonth(it)
        }
        intelligence.getAllRepresentationDrafts().first().filter { it.disputeMonth in months }.forEach {
            intelligence.deleteRepresentationDraft(it.id)
        }
        return months.size
    }

    /** Months whose stored payslip carries the seed marker. */
    suspend fun seededMonths(): Set<String> =
        dao.getAllPayslips().first()
            .mapNotNull { entity -> runCatching { entity.toDomain() }.getOrNull() }
            .filter { it.file.startsWith(SyntheticSeedPayslips.FILE_PREFIX) }
            .map { it.dateStr }
            .toSet()

    private suspend fun occupiedMonths(): Set<String> =
        dao.getAllPayslips().first().map { it.dateStr }.toSet() +
            dao.getAllLedgerRecords().first().map { it.dateStr } +
            intelligence.getAllFinancialInsights().first().map { it.monthStr } +
            intelligence.getAllRepresentationDrafts().first().map { it.disputeMonth }
}
