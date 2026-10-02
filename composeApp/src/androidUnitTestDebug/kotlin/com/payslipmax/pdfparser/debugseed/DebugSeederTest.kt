package com.payslipmax.pdfparser.debugseed

import com.payslipmax.pdfparser.database.FinancialInsightEntity
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.insights.AnomalyTierMap
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.screens.PayAuditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Phase 8: each synthetic scenario must produce, through the real [FinancialIntelligenceRepository] and the
 * real Pay Audit ViewModel, the verdict it exists to show on a device — and the seed must never touch a
 * month it did not create.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebugSeederTest {
    private lateinit var dao: FakePayslipDao
    private lateinit var intelligence: FinancialIntelligenceRepository
    private lateinit var seeder: DebugSeeder

    @BeforeTest
    fun setUp() {
        dao = FakePayslipDao()
        intelligence = FinancialIntelligenceRepository(dao, Dispatchers.Unconfined)
        seeder = DebugSeeder(dao, intelligence)
    }

    private suspend fun tptaRow(month: String) = insights().filter { it.monthStr == month && it.title == "TPTA Entitlement Advisory" }

    private suspend fun insights(): List<FinancialInsightEntity> = intelligence.getAllFinancialInsights().first()

    private suspend fun drafts(month: String) = intelligence.getAllRepresentationDrafts().first().filter { it.disputeMonth == month }

    private suspend fun storedPayslips(): List<ParsedPayslip> = PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined).getAllPayslips().first()

    /** What the Pay Audit screen shows for [month]: the real ViewModel over the stored payslips. */
    private suspend fun auditOf(
        month: PayMonth,
        hasAccess: Boolean = true,
    ) = PayAuditViewModel(dispatcher = Dispatchers.Unconfined).run {
        setInputs(storedPayslips(), hasAccess, month)
        uiState.value
    }

    private suspend fun seed(vararg steps: SeedStep) = steps.forEach { assertIs<SeedResult.Seeded>(seeder.apply(it), "seeding $it") }

    @Test
    fun theProvenScenarioShowsAnIssueWithOneDraftLetter() =
        runTest {
            seed(SeedStep.PROVEN_TPTA)

            val state = auditOf(PayMonth(2023, 3))
            assertEquals(listOf(AnomalyTierMap.TPTA_ENTITLEMENT), state.findings.issues.map { it.type })
            assertTrue(state.findings.waiting.isEmpty())
            assertEquals(1, drafts("03/2023").size, "a proven finding offers exactly one letter")
            assertEquals(1, tptaRow("03/2023").size)
        }

    @Test
    fun theHeldScenarioShowsWaitingAndNoLetter() =
        runTest {
            seed(SeedStep.HELD_TPTA)

            val state = auditOf(PayMonth(2018, 3))
            assertEquals(1, state.findings.waiting.size)
            assertTrue(state.findings.issues.isEmpty(), "a held month is not an issue")
            assertTrue(drafts("03/2018").isEmpty(), "a held finding is not proven, so no letter")
            assertTrue(tptaRow("03/2018").single().contentMarkdown.contains("on hold"))
        }

    @Test
    fun heldThenResolvedLeavesNoFindingAndNoStaleRow() =
        runTest {
            seed(SeedStep.HELD_FOR_RESOLVE)
            assertEquals(1, auditOf(PayMonth(2019, 3)).findings.waiting.size, "held before the follow-up")
            assertEquals(1, tptaRow("03/2019").size)

            seed(SeedStep.RESOLVE_BY_RELOCATION)

            val state = auditOf(PayMonth(2019, 3))
            assertTrue(state.findings.waiting.isEmpty() && state.findings.issues.isEmpty(), "a relocation explains the gap")
            assertTrue(tptaRow("03/2019").isEmpty(), "the pending row is removed by the re-audit")
            assertTrue(drafts("03/2019").isEmpty())
        }

    @Test
    fun heldThenSurfacedIsOneRowAndOneLetter() =
        runTest {
            seed(SeedStep.HELD_FOR_SURFACE)
            val heldId = tptaRow("03/2020").single().id
            assertTrue(drafts("03/2020").isEmpty())

            seed(SeedStep.SURFACE_BY_SAME_CITY)

            val state = auditOf(PayMonth(2020, 3))
            assertEquals(1, state.findings.issues.size)
            assertTrue(state.findings.waiting.isEmpty())
            assertEquals(heldId, tptaRow("03/2020").single().id, "the same row, not a second one")
            assertEquals(1, drafts("03/2020").size)
        }

    @Test
    fun theIncrementScenarioShowsAMissedIncrementWithOneLetter() =
        runTest {
            seed(SeedStep.INCREMENT_MISS)

            val state = auditOf(PayMonth(2022, 7))
            assertEquals(listOf(AnomalyTierMap.INCREMENT_MISSED), state.findings.issues.map { it.type })
            assertEquals(1, drafts("07/2022").size)
        }

    @Test
    fun allScenariosTogetherDoNotDisturbEachOther() =
        runTest {
            SeedStep.entries.forEach { seed(it) }

            val seen =
                storedPayslips().flatMap { slip ->
                    val state = auditOf(PayMonth(slip.year, slip.monthNum))
                    state.findings.issues.map { Triple(slip.dateStr, it.type, "issue") } +
                        state.findings.waiting.map { Triple(slip.dateStr, it.type, "waiting") }
                }.toSet()
            assertEquals(
                setOf(
                    Triple("03/2023", AnomalyTierMap.TPTA_ENTITLEMENT, "issue"),
                    Triple("03/2018", AnomalyTierMap.TPTA_ENTITLEMENT, "waiting"),
                    Triple("03/2020", AnomalyTierMap.TPTA_ENTITLEMENT, "issue"),
                    Triple("07/2022", AnomalyTierMap.INCREMENT_MISSED, "issue"),
                ),
                seen,
            )
        }

    @Test
    fun theFreeTierSeesTheCountButNotTheEvidence() =
        runTest {
            seed(SeedStep.PROVEN_TPTA)

            val state = auditOf(PayMonth(2023, 3), hasAccess = false)
            assertTrue(state.isLocked)
            assertTrue(state.findings.issues.isEmpty())
            assertEquals(1, state.hiddenFindingCount)
        }

    @Test
    fun everySeededPayslipIsMarkedAndSyntheticAndFarFromRealMonths() {
        SeedStep.entries.flatMap { SyntheticSeedPayslips.payslips(it) }.also { assertTrue(it.isNotEmpty()) }.forEach {
            assertTrue(it.file.startsWith(SyntheticSeedPayslips.FILE_PREFIX), "marker on ${it.dateStr}")
            assertTrue(it.year <= 2023, "seed months stay before any real payslip era: ${it.dateStr}")
            assertEquals(Officer("Seed Officer", "00/000/000000X", "AAAAA0000A"), it.officer, "placeholder officer only")
        }
    }

    @Test
    fun aSeedStepRefusesToTouchAMonthThatAlreadyExists() =
        runTest {
            val real = realPayslip(2018, 3)
            dao.insertPayslip(real.toEncryptedEntity())

            val result = seeder.apply(SeedStep.HELD_TPTA)

            assertEquals(SeedResult.Collision(listOf("03/2018")), result)
            assertEquals(listOf("03/2018"), storedPayslips().map { it.dateStr }, "nothing else was written")
            assertTrue(insights().isEmpty() && dao.getAllLedgerRecords().first().isEmpty())
            assertEquals("real.pdf", storedPayslips().single().file, "the real month is untouched")
        }

    @Test
    fun anOrphanDraftOrInsightOnATargetMonthAlsoBlocksTheSeed() =
        runTest {
            dao.insertFinancialInsight(FinancialInsightEntity("x", "02/2018", "INFO", "t", "c", "LOW", 1L, false))

            assertEquals(SeedResult.Collision(listOf("02/2018")), seeder.apply(SeedStep.HELD_TPTA))
            assertTrue(storedPayslips().isEmpty())
        }

    @Test
    fun seedingTheSameStepTwiceIsRefusedTheSecondTime() =
        runTest {
            seed(SeedStep.HELD_TPTA)

            assertIs<SeedResult.Collision>(seeder.apply(SeedStep.HELD_TPTA))
            assertEquals(3, storedPayslips().size)
        }

    @Test
    fun aFollowUpNeedsItsBaseSeededFirst() =
        runTest {
            assertEquals(SeedResult.MissingPrerequisite(SeedStep.HELD_FOR_RESOLVE), seeder.apply(SeedStep.RESOLVE_BY_RELOCATION))
            assertTrue(storedPayslips().isEmpty())
        }

    @Test
    fun removeDeletesOnlySeededMonthsAndEverythingTheySpawned() =
        runTest {
            val real = realPayslip(2025, 8)
            dao.insertPayslip(real.toEncryptedEntity())
            intelligence.processPayslipAndRunAnalysis(real)
            val realInsights = insights()
            seed(*SeedStep.entries.toTypedArray())
            assertTrue(drafts("03/2023").isNotEmpty())

            val removed = seeder.remove()

            assertEquals(SeedStep.entries.flatMap { SyntheticSeedPayslips.payslips(it) }.size, removed)
            assertEquals(listOf("08/2025"), storedPayslips().map { it.dateStr })
            assertEquals(listOf("08/2025"), dao.getAllLedgerRecords().first().map { it.dateStr })
            assertEquals(realInsights, insights(), "the real month's insights are untouched")
            assertTrue(intelligence.getAllRepresentationDrafts().first().isEmpty())
            assertTrue(seeder.seededMonths().isEmpty())
        }

    @Test
    fun seededMonthsListsOnlyMarkedPayslips() =
        runTest {
            dao.insertPayslip(realPayslip(2025, 8).toEncryptedEntity())
            seed(SeedStep.HELD_TPTA)

            assertEquals(setOf("01/2018", "02/2018", "03/2018"), seeder.seededMonths())
        }

    private fun realPayslip(
        year: Int,
        month: Int,
    ) = ParsedPayslip(
        file = "real.pdf",
        year = year,
        monthNum = month,
        monthName = "M",
        dateStr = "${month.toString().padStart(2, '0')}/$year",
        officer = Officer("Real Name", "11/111/111111X", "BBBBB1111B"),
        earnings = Earnings(basicPay = 100000.0),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0),
        taxAndSavings = null,
    )
}
