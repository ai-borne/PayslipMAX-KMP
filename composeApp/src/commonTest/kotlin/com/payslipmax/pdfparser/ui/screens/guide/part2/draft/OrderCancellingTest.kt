package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.CancelDrafts
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * WHY: "Cancel and re-draft" (Part II plan phase 6c, DECISIONS Round 22) must make the cancel slip and a corrected draft together or not
 * at all: a half-made pair would leave a draft pointing at nothing, and a cancel slip quoting an order the officer never typed would be
 * wrong on paper. The old draft and the order are never touched. Synthetic orders only.
 */
class OrderCancellingTest {
    private val date = CalendarDate(2026, 6, 20)
    private val ref = OrderRef("0123/26", date)
    private var ids = 0
    private var clock = 7_000L

    /** Refuses a draft with this id, as a full device would, and records which drafts were deleted. */
    private class Refusing(
        private val refuse: String,
        private val inner: FakePart2Repository = FakePart2Repository(),
    ) : Part2Repository by inner {
        override suspend fun saveDraft(draft: Part2Draft) = if (draft.id == refuse) Part2WriteResult.TOO_MANY else inner.saveDraft(draft)
    }

    private fun cancelling(
        repo: Part2Repository,
        section: com.payslipmax.pdfparser.guide.model.Part2Lookup? = SyntheticPart2Templates.lookup,
    ) = OrderCancelling(section, repo, SyntheticPart2Templates.registry(), { clock }, { "d-${++ids}" })

    private suspend fun Part2Repository.drafts() = observeDrafts().first().items.associateBy { it.id }

    private val source =
        Part2Draft(
            "src",
            LeaveEventInput(listOf(OccurrenceRequest(GRANT, mapOf("year" to SlotValue.NumberValue(2026)), setOf("on_ration")))),
            mapOf(GRANT to "old-rev"),
            100,
            200,
        )

    private suspend fun Part2Repository.seed(
        no: String? = "0123/26",
        orderDate: CalendarDate? = date,
        draftId: String? = "src",
        withDraft: Boolean = true,
    ) {
        if (withDraft) saveDraft(source)
        saveOrder(VaultOrder("o-1", draftId, no, orderDate, 1, 1))
    }

    @Test
    fun cancellingMakesOneSlipQuotingTheOrderAndNothingElse() =
        runTest {
            val repo = FakePart2Repository().apply { seed(draftId = null, withDraft = false) }

            val outcome = cancelling(repo).cancel("o-1", redraft = false)

            assertEquals(CancelOutcome.Started("d-1"), outcome)
            val saved = repo.drafts().values.single()
            assertEquals(Part2Draft("d-1", CancelDrafts.cancelEvent(ref), emptyMap(), 7_000, 7_000, null, null), saved)
            assertEquals(VaultOrder("o-1", null, "0123/26", date, 1, 1), repo.order("o-1"), "the order is untouched")
        }

    @Test
    fun cancelAndRedraftMakesTheSlipAndACopyOfTheLinkedDraftPointingAtEachOther() =
        runTest {
            val repo = FakePart2Repository().apply { seed() }

            val outcome = cancelling(repo).cancel("o-1", redraft = true)

            assertEquals(CancelOutcome.Started("d-1"), outcome, "the officer lands on the cancel slip")
            val drafts = repo.drafts()
            assertEquals(setOf("src", "d-1", "d-2"), drafts.keys)
            assertEquals("d-2", drafts.getValue("d-1").pairedWith)
            assertEquals("d-1", drafts.getValue("d-2").pairedWith)
            assertEquals(CancelDrafts.cancelEvent(ref), drafts.getValue("d-1").event)
            assertEquals(CancelDrafts.redraftEvent(source.event), drafts.getValue("d-2").event)
            assertEquals(source, drafts.getValue("src"), "the old draft is never touched")
        }

    @Test
    fun theCorrectedDraftIsAFreshDraftWithTheCurrentTemplateRevisionAndNoPrintRecord() =
        runTest {
            val repo = FakePart2Repository().apply { seed() }

            cancelling(repo).cancel("o-1", redraft = true)

            val copy = repo.drafts().getValue("d-2")
            assertEquals(mapOf(GRANT to SyntheticPart2Templates.templates.first { it.code == GRANT }.rev), copy.templateRevs)
            assertEquals(7_000L to 7_000L, copy.createdAt to copy.updatedAt)
            assertNull(copy.printed)
        }

    @Test
    fun theCorrectedDraftDoesNotCarryTheOldDraftsLastOrderLineBecauseTheVaultReadsThatLive() =
        runTest {
            val repo = FakePart2Repository().apply { seed() }
            repo.saveDraft(source.copy(event = source.event.copy(lastOrder = OrderRef("0001/26", CalendarDate(2026, 1, 2)))))

            cancelling(repo).cancel("o-1", redraft = true)

            assertNull(repo.drafts().getValue("d-2").event.lastOrder)
        }

    @Test
    fun anOrderWithoutAQuotableNumberAndDateIsRefusedAndNothingIsSaved() =
        runTest {
            for ((no, d) in listOf<Pair<String?, CalendarDate?>>(null to date, "0123/26" to null, "" to date, "a<b" to date)) {
                val repo = FakePart2Repository().apply { seed(no, d) }
                val outcome = cancelling(repo).cancel("o-1", redraft = true)

                assertEquals(CancelOutcome.Refused(CancelRefusal.NOT_QUOTABLE), outcome, "no=$no")
                assertEquals(setOf("src"), repo.drafts().keys, "no half pair")
            }
        }

    @Test
    fun anOrderThatIsGoneAndAGuideThatIsNotLoadedAreRefusedWithTheirOwnReason() =
        runTest {
            val repo = FakePart2Repository().apply { seed() }

            assertEquals(CancelOutcome.Refused(CancelRefusal.GONE), cancelling(repo).cancel("missing", false))
            assertEquals(CancelOutcome.Refused(CancelRefusal.NOT_READY), cancelling(repo, section = null).cancel("o-1", false))
            assertEquals(setOf("src"), repo.drafts().keys)
        }

    @Test
    fun aRedraftNeedsTheDraftTheOrderCameFromAndSaysSoWhenThereIsNone() =
        runTest {
            for (repo in listOf(FakePart2Repository().apply { seed(draftId = null) }, FakePart2Repository().apply { seed(draftId = "gone", withDraft = false) })) {
                val outcome = cancelling(repo).cancel("o-1", redraft = true)

                assertEquals(CancelOutcome.Refused(CancelRefusal.NOTHING_TO_COPY), outcome)
                assertEquals(emptySet(), repo.drafts().keys - "src", "no cancel slip is made when the corrected draft cannot be")
            }
        }

    @Test
    fun anOrderThatCameFromACancelSlipHasNothingToRedraftAndNothingIsSaved() =
        runTest {
            val repo = FakePart2Repository().apply { seed(withDraft = false) }
            repo.saveDraft(Part2Draft("src", CancelDrafts.cancelEvent(ref), emptyMap(), 100, 200))

            assertEquals(CancelOutcome.Refused(CancelRefusal.CANCEL_SLIP), cancelling(repo).cancel("o-1", redraft = true))
            assertEquals(setOf("src"), repo.drafts().keys)
            assertIs<CancelOutcome.Started>(cancelling(repo).cancel("o-1", redraft = false), "the order can still be cancelled on its own")
        }

    @Test
    fun ifTheCancelSlipCannotBeSavedTheCorrectedDraftMadeForItIsRemoved() =
        runTest {
            val repo = Refusing(refuse = "d-1").apply { seed() }

            val outcome = cancelling(repo).cancel("o-1", redraft = true)

            assertEquals(CancelOutcome.Refused(CancelRefusal.SAVE_FAILED), outcome)
            assertEquals(setOf("src"), repo.drafts().keys, "no orphan pointing at a slip that does not exist")
        }

    @Test
    fun ifTheCorrectedDraftCannotBeSavedNothingIsMade() =
        runTest {
            val repo = Refusing(refuse = "d-2").apply { seed() }

            assertEquals(CancelOutcome.Refused(CancelRefusal.SAVE_FAILED), cancelling(repo).cancel("o-1", redraft = true))
            assertEquals(setOf("src"), repo.drafts().keys)
        }

    @Test
    fun aCancelOnItsOwnThatCannotBeSavedSaysSo() =
        runTest {
            val repo = Refusing(refuse = "d-1").apply { seed(draftId = null, withDraft = false) }

            assertIs<CancelOutcome.Refused>(cancelling(repo).cancel("o-1", redraft = false))
            assertEquals(emptySet(), repo.drafts().keys)
        }
}
