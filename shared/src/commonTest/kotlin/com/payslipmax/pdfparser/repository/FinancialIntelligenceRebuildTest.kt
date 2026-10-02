package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.payAuditPayslip
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Payslips are the single source: the ledger, insights and drafted letters are derived, so after a restore
 * (or a repair) they must come out exactly as if the officer had imported the same months one by one.
 *
 * Fixture: Jan/Feb 2018 pay TPTA at the other-places rate, Mar pays none (held), Apr pays the same rate, so
 * March's gap is a proven shortfall that drafts one letter.
 */
class FinancialIntelligenceRebuildTest {
    private val otherCityTpta = 3600.0 * 1.17

    private fun slip(
        month: Int,
        tpta: Double = otherCityTpta,
    ): ParsedPayslip = payAuditPayslip(2018, month, 85300.0, tpta = tpta)

    private val provenMarch = listOf(slip(1), slip(2), slip(3, tpta = 0.0), slip(4), slip(5), slip(6))

    private fun repo(dao: PayslipDao) = FinancialIntelligenceRepository(dao, Dispatchers.Unconfined)

    private suspend fun FakePayslipDao.letters(month: String = "03/2018") = getAllRepresentationDrafts().first().filter { it.disputeMonth == month }

    @Test
    fun rebuildGivesExactlyWhatImportingTheSameMonthsOneByOneGives() =
        runTest {
            val imported = FakePayslipDao()
            provenMarch.forEach { repo(imported).processPayslipAndRunAnalysis(it) }
            val rebuilt = FakePayslipDao()

            repo(rebuilt).rebuildAuditHistory(provenMarch)

            assertEquals(imported.getAllLedgerRecords().first(), rebuilt.getAllLedgerRecords().first(), "one ledger row per payslip")
            assertEquals(imported.insightShape(), rebuilt.insightShape(), "insights must match a fresh import")
            assertEquals(imported.letters().map { it.bodyText }, rebuilt.letters().map { it.bodyText })
            assertEquals(1, rebuilt.letters().size)
        }

    private suspend fun FakePayslipDao.insightShape() = getAllFinancialInsights().first().map { listOf(it.id, it.monthStr, it.title, it.contentMarkdown, it.severity) }.toSet()

    @Test
    fun rebuildUpsertsALedgerRowForEveryPayslipEvenWhenOnlySomeExist() =
        runTest {
            val dao = FakePayslipDao()
            repo(dao).processPayslipAndRunAnalysis(provenMarch[0])
            repo(dao).processPayslipAndRunAnalysis(provenMarch[1])

            repo(dao).rebuildAuditHistory(provenMarch)

            assertEquals(provenMarch.map { it.dateStr }, dao.getAllLedgerRecords().first().map { it.dateStr })
        }

    @Test
    fun aRestoredLetterForAStillProvenFindingKeepsTheOfficersEdits() =
        runTest {
            val dao = FakePayslipDao()
            repo(dao).rebuildAuditHistory(provenMarch)
            val edited = dao.letters().single().copy(bodyText = "Edited by the officer")
            dao.insertRepresentationDraft(edited)

            repo(dao).rebuildAuditHistory(provenMarch)

            assertEquals(listOf(edited), dao.letters(), "the edit survives and no second letter is stacked")
        }

    @Test
    fun aLetterWhoseFindingNoLongerAppliesIsRemoved() =
        runTest {
            val dao = FakePayslipDao()
            val stale = RepresentationDraftEntity("old", "03/2018", "TPTA_ENTITLEMENT", "PCDA_O_PUNE", "s", "b", 1L)
            dao.insertRepresentationDraft(stale)
            val resolvedMarch = listOf(slip(1), slip(2), slip(3, tpta = 0.0), slip(4, tpta = 7200.0 * 1.17), slip(5, tpta = 7200.0 * 1.17))

            repo(dao).rebuildAuditHistory(resolvedMarch)

            assertTrue(dao.letters().isEmpty(), "April shows a higher city class, so March is explained and its letter must go")
        }

    @Test
    fun aDeletedLetterRecordStopsTheRebuildFromDraftingItAgain() =
        runTest {
            val dao = FakePayslipDao()
            dao.insertDismissedDraft(DismissedDraftEntity("03/2018", "TPTA_ENTITLEMENT"))

            repo(dao).rebuildAuditHistory(provenMarch)

            assertTrue(dao.letters().isEmpty(), "the officer deleted this letter; a rebuild must not bring it back")
            assertEquals(listOf(DismissedDraftEntity("03/2018", "TPTA_ENTITLEMENT")), dao.getAllDismissedDrafts(), "the record stays while the finding stands")
        }

    @Test
    fun repairRebuildsWhenAPayslipHasNoLedgerRowAndThenLeavesACompleteLedgerAlone() =
        runTest {
            val dao = CountingDao(FakePayslipDao())
            repo(dao).processPayslipAndRunAnalysis(provenMarch[0])
            repo(dao).processPayslipAndRunAnalysis(provenMarch[1])
            dao.audits = 0

            assertTrue(repo(dao).repairAuditHistoryIfIncomplete(provenMarch), "2 of 6 ledger rows: must rebuild")
            assertEquals(6, dao.getAllLedgerRecords().first().size)
            assertEquals(6, dao.audits, "each month is audited once")

            dao.audits = 0
            assertFalse(repo(dao).repairAuditHistoryIfIncomplete(provenMarch), "a complete ledger needs no work")
            assertEquals(0, dao.audits, "and nothing may be audited")
        }

    @Test
    fun aFailingRepairNeverThrowsAndLeavesPayslipsAndSettingsUntouched() =
        runTest {
            val fake = FakePayslipDao()
            fake.insertSettings(AppSettingsEntity(appTheme = "dark"))
            val dao = CountingDao(fake).also { it.failInsights = true }

            val ran = repo(dao).repairAuditHistoryIfIncomplete(provenMarch)

            assertFalse(ran, "a failed repair reports it did not complete, and does not throw")
            assertEquals("dark", fake.getSettings()?.appTheme)
        }

    /** Counts audits (each reads its month's insights once) and can fail the insight write. */
    private class CountingDao(
        private val delegate: FakePayslipDao,
    ) : PayslipDao by delegate {
        var audits = 0
        var failInsights = false

        override suspend fun getFinancialInsightsByMonth(monthStr: String): List<FinancialInsightEntity> {
            audits++
            return delegate.getFinancialInsightsByMonth(monthStr)
        }

        override suspend fun insertFinancialInsights(insights: List<FinancialInsightEntity>) {
            check(!failInsights) { "disk full" }
            delegate.insertFinancialInsights(insights)
        }
    }
}
