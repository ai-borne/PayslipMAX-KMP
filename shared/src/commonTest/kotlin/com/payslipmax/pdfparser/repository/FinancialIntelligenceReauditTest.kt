package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.FinancialInsightEntity
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.payAuditPayslip
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 7 (docs/Plan/09_PayAudit_PhasePlan.md): importing a payslip re-audits the earlier months whose
 * verdict it can change, so a held (pending) TPTA finding either resolves (no stale pending row) or
 * surfaces once under the same id, and only a proven finding ever drafts a letter.
 *
 * Fixture: Jan/Feb 2018 pay TPTA at the other-places rate, Mar 2018 pays none. With nothing after March
 * the gap could still be a relocation, so it is held; the April payslip settles it.
 */
class FinancialIntelligenceReauditTest {
    private lateinit var dao: FakePayslipDao
    private lateinit var repository: FinancialIntelligenceRepository

    @BeforeTest
    fun setUp() {
        dao = FakePayslipDao()
        repository = FinancialIntelligenceRepository(payslipDao = dao, dispatcher = Dispatchers.Unconfined)
    }

    private val otherCityTpta = 3600.0 * 1.17
    private val higherCityTpta = 7200.0 * 1.17

    private fun slip(
        year: Int,
        month: Int,
        tpta: Double,
    ): ParsedPayslip = payAuditPayslip(year, month, 85300.0, tpta = tpta)

    private suspend fun importMonths(vararg slips: ParsedPayslip) = slips.forEach { repository.processPayslipAndRunAnalysis(it) }

    private suspend fun heldMarch() = importMonths(slip(2018, 1, otherCityTpta), slip(2018, 2, otherCityTpta), slip(2018, 3, 0.0))

    private suspend fun tptaRows(month: String) = repository.getAllFinancialInsights().first().filter { it.monthStr == month && it.title == "TPTA Entitlement Advisory" }

    private suspend fun drafts(month: String) = repository.getAllRepresentationDrafts().first().filter { it.disputeMonth == month }

    private fun tptaRowId(month: String) = CryptoHelper.sha256("$month-TPTA_ENTITLEMENT-transportAllowance")

    @Test
    fun aHeldFindingIsStoredWithoutADraftLetter() =
        runTest {
            heldMarch()

            val row = tptaRows("03/2018").single()
            assertTrue(row.contentMarkdown.contains("on hold"), "the held row says it is on hold: ${row.contentMarkdown}")
            assertTrue(drafts("03/2018").isEmpty(), "a held finding is never proven, so it must not draft a letter")
        }

    @Test
    fun aLaterRelocationResolvesTheHeldRowSoNoStalePendingRowIsLeft() =
        runTest {
            heldMarch()

            importMonths(slip(2018, 4, higherCityTpta))

            assertTrue(tptaRows("03/2018").isEmpty(), "April shows the higher city class, so March is explained; its pending row must go")
            assertTrue(drafts("03/2018").isEmpty())
        }

    @Test
    fun aLaterPayslipThatRulesOutRelocationSurfacesTheFindingOnceUnderTheSameId() =
        runTest {
            heldMarch()
            val heldId = tptaRows("03/2018").single().id

            importMonths(slip(2018, 4, otherCityTpta))

            val row = tptaRows("03/2018").single()
            assertEquals(heldId, row.id, "the surfaced finding keeps the held row's id, so it is one row, not two")
            assertEquals(tptaRowId("03/2018"), row.id)
            assertTrue(!row.contentMarkdown.contains("on hold"), "no longer held: ${row.contentMarkdown}")
            assertTrue(row.contentMarkdown.contains("a month is due"), "surfaced with the amount due: ${row.contentMarkdown}")
        }

    @Test
    fun aSurfacedFindingDraftsExactlyOneLetterEvenAcrossFurtherImports() =
        runTest {
            heldMarch()
            importMonths(slip(2018, 4, otherCityTpta))
            assertEquals(1, drafts("03/2018").size, "proven once surfaced, so one letter")

            importMonths(slip(2018, 5, otherCityTpta), slip(2018, 6, otherCityTpta))

            assertEquals(1, drafts("03/2018").size, "re-auditing the same month again must not stack duplicate letters")
            assertEquals(1, tptaRows("03/2018").size)
        }

    @Test
    fun importingTheSameProvenMonthTwiceKeepsOneLetter() =
        runTest {
            importMonths(slip(2018, 1, otherCityTpta), slip(2018, 2, otherCityTpta))
            importMonths(slip(2018, 3, 0.0), slip(2018, 4, otherCityTpta))
            importMonths(slip(2018, 4, otherCityTpta))

            assertEquals(1, drafts("03/2018").size)
        }

    @Test
    fun reauditRewritesAnOldRowInPlainWordsButKeepsItsCreatedAtAndArchivedFlag() =
        runTest {
            heldMarch()
            val old = tptaRows("03/2018").single()
            dao.insertFinancialInsight(old.copy(contentMarkdown = "old raw wording 3600 -> 0", createdAt = 1L, isArchived = true))

            importMonths(slip(2018, 4, otherCityTpta))

            val row = tptaRows("03/2018").single()
            assertTrue(row.contentMarkdown.contains("a month is due"), "rewritten with the current wording: ${row.contentMarkdown}")
            assertEquals(1L, row.createdAt, "the row's age is kept")
            assertTrue(row.isArchived, "the officer's archive choice is kept")
        }

    @Test
    fun aPayslipMoreThanSixMonthsLaterLeavesTheOldRowAlone() =
        runTest {
            heldMarch()
            val stale: FinancialInsightEntity = tptaRows("03/2018").single().copy(contentMarkdown = "untouched")
            dao.insertFinancialInsight(stale)

            importMonths(slip(2018, 10, otherCityTpta))

            assertEquals(stale, tptaRows("03/2018").single(), "October is outside March's six-month window")
            assertTrue(drafts("03/2018").isEmpty())
        }

    @Test
    fun aDeletedMonthIsNotBroughtBackByALaterImport() =
        runTest {
            heldMarch()
            dao.deleteLedgerRecord("03/2018")
            dao.deleteFinancialInsightsByMonth("03/2018")

            importMonths(slip(2018, 4, otherCityTpta))

            assertTrue(repository.getAllFinancialInsights().first().none { it.monthStr == "03/2018" })
            assertTrue(drafts("03/2018").isEmpty())
        }

    @Test
    fun theImportedMonthsOwnResultIsStillReturned() =
        runTest {
            heldMarch()

            val result = repository.processPayslipAndRunAnalysis(slip(2018, 4, otherCityTpta))

            assertTrue(result.anomalies.none { it.type == "TPTA_ENTITLEMENT" }, "April itself pays TPTA; the March finding is not April's")
        }

    // Backfill: an older payslip imported later can change a later month's verdict. With no earlier payslip,
    // March's missing TPTA reads as proven; once February (paying TPTA) exists and nothing after March does,
    // a relocation can no longer be ruled out, so March is only held.
    @Test
    fun importingAnEarlierPayslipLaterReauditsTheMonthsAfterIt() =
        runTest {
            importMonths(slip(2018, 3, 0.0))
            assertTrue(!tptaRows("03/2018").single().contentMarkdown.contains("on hold"), "alone, March reads as proven")
            assertEquals(1, drafts("03/2018").size)

            importMonths(slip(2018, 2, otherCityTpta))

            assertTrue(tptaRows("03/2018").single().contentMarkdown.contains("on hold"), "February's payslip makes March held")
            assertTrue(drafts("03/2018").isEmpty(), "a held finding is not proven, so its letter goes")
        }

    // Stale letters (user decision 2026-10-02): once the finding is addressed its letter is gone.
    @Test
    fun aLetterIsRemovedOnceItsFindingNoLongerAppliesAfterReimport() =
        runTest {
            importMonths(slip(2018, 1, otherCityTpta), slip(2018, 2, otherCityTpta), slip(2018, 3, 0.0), slip(2018, 4, otherCityTpta))
            assertEquals(1, drafts("03/2018").size, "proven, so a letter exists")

            importMonths(slip(2018, 3, otherCityTpta))

            assertTrue(tptaRows("03/2018").isEmpty(), "March now pays TPTA")
            assertTrue(drafts("03/2018").isEmpty(), "the finding was addressed, so its letter is removed")
        }

    @Test
    fun aLetterForAStillProvenFindingIsKeptAndItsEditsSurvive() =
        runTest {
            importMonths(slip(2018, 1, otherCityTpta), slip(2018, 2, otherCityTpta), slip(2018, 3, 0.0), slip(2018, 4, otherCityTpta))
            val letter = drafts("03/2018").single()
            dao.insertRepresentationDraft(letter.copy(bodyText = "edited by the officer"))

            importMonths(slip(2018, 5, otherCityTpta))

            assertEquals("edited by the officer", drafts("03/2018").single().bodyText)
        }

    @Test
    fun aLetterOfAnOlderTypeThatTheAuditNeverDraftsIsLeftAlone() =
        runTest {
            heldMarch()
            val legacy =
                com.payslipmax.pdfparser.database.RepresentationDraftEntity("legacy", "03/2018", "SALARY_DROP", "PCDA", "s", "b", 1L)
            dao.insertRepresentationDraft(legacy)

            importMonths(slip(2018, 4, higherCityTpta))

            assertEquals(listOf(legacy), drafts("03/2018"))
        }

    // Deleted letters (user decision 2026-10-02): a letter the officer deletes stays gone while its finding stands.
    private suspend fun provenMarch() =
        importMonths(slip(2018, 1, otherCityTpta), slip(2018, 2, otherCityTpta), slip(2018, 3, 0.0), slip(2018, 4, otherCityTpta))

    @Test
    fun aLetterTheOfficerDeletedDoesNotComeBackOnTheNextReaudit() =
        runTest {
            provenMarch()
            repository.deleteRepresentationDraft(drafts("03/2018").single().id)
            assertTrue(drafts("03/2018").isEmpty())

            importMonths(slip(2018, 5, otherCityTpta))
            importMonths(slip(2018, 4, otherCityTpta))

            assertTrue(drafts("03/2018").isEmpty(), "the finding still stands, but the officer chose to delete its letter")
            assertEquals(1, tptaRows("03/2018").size, "the finding itself is still shown")
        }

    @Test
    fun deletingOneLetterDoesNotSuppressAnotherMonthsLetter() =
        runTest {
            provenMarch()
            importMonths(slip(2018, 5, 0.0), slip(2018, 6, otherCityTpta))
            assertEquals(1, drafts("05/2018").size)

            repository.deleteRepresentationDraft(drafts("03/2018").single().id)
            importMonths(slip(2018, 7, otherCityTpta))

            assertEquals(1, drafts("05/2018").size, "May's letter is untouched")
        }

    @Test
    fun aFindingThatResolvesAndIsProvenAgainDraftsAFreshLetter() =
        runTest {
            provenMarch()
            repository.deleteRepresentationDraft(drafts("03/2018").single().id)
            importMonths(slip(2018, 3, otherCityTpta))
            assertTrue(tptaRows("03/2018").isEmpty(), "March now pays TPTA, so the finding is addressed")

            importMonths(slip(2018, 3, 0.0))

            assertEquals(1, drafts("03/2018").size, "a new finding is not suppressed by the old deletion")
        }

    @Test
    fun deletingALetterOfAnUnauditedTypeLeavesNoRecord() =
        runTest {
            val legacy = com.payslipmax.pdfparser.database.RepresentationDraftEntity("legacy", "03/2018", "SALARY_DROP", "PCDA", "s", "b", 1L)
            dao.insertRepresentationDraft(legacy)

            repository.deleteRepresentationDraft("legacy")

            assertTrue(dao.getAllDismissedDrafts().isEmpty())
        }
}
