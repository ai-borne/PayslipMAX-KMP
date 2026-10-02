package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.insights.payAuditPayslip
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A backup carries only what the officer created; the ledger and insights are derived, so a restore must
 * rebuild them from the restored payslips (the backup never stores them), through the real wiring:
 * service -> PayslipRepository (corrections applied) -> FinancialIntelligenceRepository.
 */
class PayslipBackupServiceRebuildTest {
    private lateinit var source: FakePayslipDao
    private lateinit var target: FakePayslipDao
    private lateinit var sourceService: PayslipBackupService
    private lateinit var targetService: PayslipBackupService

    private val otherCityTpta = 3600.0 * 1.17
    private val months = listOf(1, 2, 3, 4, 5, 6).map { payAuditPayslip(2018, it, 85300.0, tpta = if (it == 3) 0.0 else otherCityTpta) }

    private fun serviceFor(dao: FakePayslipDao) =
        PayslipBackupService(
            dao,
            Dispatchers.Unconfined,
            PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined),
            FinancialIntelligenceRepository(dao, Dispatchers.Unconfined),
        )

    @BeforeTest
    fun setUp() {
        source = FakePayslipDao()
        target = FakePayslipDao()
        sourceService = serviceFor(source)
        targetService = serviceFor(target)
    }

    private suspend fun seedSource() {
        months.forEach { source.insertPayslip(it.toEncryptedEntity()) }
        FinancialIntelligenceRepository(source, Dispatchers.Unconfined).rebuildAuditHistory(months)
    }

    private suspend fun restoreFromSource(mode: RestoreMode = RestoreMode.REPLACE) = targetService.restore(sourceService.export(BACKUP_PASSWORD).getOrThrow(), BACKUP_PASSWORD, mode).getOrThrow()

    @Test
    fun replaceRestoreRebuildsOneLedgerRowPerPayslipAndTheirInsights() =
        runTest {
            seedSource()
            target.seedDeviceMonth("01/2023")

            restoreFromSource()

            assertEquals(months.map { it.dateStr }, target.getAllLedgerRecords().first().map { it.dateStr }, "the old device's month must be gone")
            assertEquals(
                source.getAllFinancialInsights().first().map { it.id }.toSet(),
                target.getAllFinancialInsights().first().map { it.id }.toSet(),
                "insights must match the source device's",
            )
        }

    @Test
    fun aRestoredEditedLetterKeepsItsEditsAndIsNotDraftedTwice() =
        runTest {
            seedSource()
            val letter = source.getAllRepresentationDrafts().first().single().copy(bodyText = "Edited by the officer")
            source.insertRepresentationDraft(letter)

            restoreFromSource()

            assertEquals(listOf(letter), target.getAllRepresentationDrafts().first())
        }

    @Test
    fun aRestoredDeletedLetterRecordStopsTheRebuildFromDraftingTheLetterAgain() =
        runTest {
            seedSource()
            source.deleteRepresentationDraft("${source.getAllRepresentationDrafts().first().single().id}")
            source.insertDismissedDraft(DismissedDraftEntity("03/2018", "TPTA_ENTITLEMENT"))

            restoreFromSource()

            assertTrue(target.getAllRepresentationDrafts().first().isEmpty())
            assertEquals(1, target.getAllDismissedDrafts().size)
        }

    @Test
    fun theRebuiltLedgerUsesTheOfficersCorrections() =
        runTest {
            seedSource()
            source.insertCorrection(mapOf("basicPay" to 99999.0).toCorrectionEntity("02/2018"))

            restoreFromSource()

            assertEquals(99999.0, target.getLedgerRecordByDate("02/2018")?.basicPay, "the ledger must carry the corrected value Pay Audit shows")
        }

    @Test
    fun mergeThenRebuildKeepsOneLetterWhenTheDeviceAndTheBackupDisagreeAboutIt() =
        runTest {
            seedSource()
            source.deleteRepresentationDraft(source.getAllRepresentationDrafts().first().single().id)
            source.insertDismissedDraft(DismissedDraftEntity("03/2018", "TPTA_ENTITLEMENT"))
            val deviceLetter = RepresentationDraftEntity("device-letter", "03/2018", "TPTA_ENTITLEMENT", "PCDA_O_PUNE", "s", "device text", 1L)
            target.insertRepresentationDraft(deviceLetter)

            restoreFromSource(RestoreMode.MERGE)

            assertEquals(listOf(deviceLetter), target.getAllRepresentationDrafts().first(), "a proven finding keeps its one letter; none is stacked")
        }

    @Test
    fun aRestoreStillSucceedsWhenTheRebuildFailsSoTheStartupRepairCanFinishIt() =
        runTest {
            seedSource()
            val broken = serviceWithFailingRebuild()

            val result = broken.restore(sourceService.export(BACKUP_PASSWORD).getOrThrow(), BACKUP_PASSWORD)

            assertTrue(result.isSuccess, "the user's data is already restored; a derived-data failure must not report a failed restore")
            assertEquals(months.size, target.getAllPayslips().first().size)
        }

    private fun serviceWithFailingRebuild() =
        PayslipBackupService(
            target,
            Dispatchers.Unconfined,
            PayslipRepository(target, FakePdfParser(), Dispatchers.Unconfined),
            object : FinancialIntelligenceRepository(target, Dispatchers.Unconfined) {
                override suspend fun rebuildAuditHistory(payslips: List<com.payslipmax.pdfparser.domain.ParsedPayslip>) = error("boom")
            },
        )
}
