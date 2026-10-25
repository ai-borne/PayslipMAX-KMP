package com.payslipmax.pdfparser.ui.screens.guide.part2

import android.content.Context
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.getDatabaseBuilder
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.RoomPart2Repository
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueCardModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueUiState
import com.payslipmax.pdfparser.ui.screens.guide.part2.due.DueViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the Due screens' own tests run over an in-memory fake. This runs the Due list, a dismissal and the Dashboard card over the production
 * schema and the real encrypted repository, so what the officer has stored, what a dismissal writes back and what survives closing and
 * opening the app are the real thing (Phase 11 item 36). Synthetic drafts and orders only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Part2DueRoomTest {
    private var database: PayslipDatabase? = null
    private val india = 19_800_000L
    private val now = PreClaimDates.toUtcMillis(CalendarDate(2026, 10, 12)) + 9 * 3_600_000L - india
    private val printedAt = now - 40L * 86_400_000L

    @Before
    fun setUp() {
        ContextHolder.context = ApplicationProvider.getApplicationContext<Context>()
    }

    @After
    fun tearDown() {
        database?.close()
        ContextHolder.context = null
    }

    private fun open(): PayslipDatabase = getDatabaseBuilder().setDriver(AndroidSQLiteDriver()).build().also { database = it }

    private fun reopen(): PayslipDatabase {
        database?.close()
        return open()
    }

    private fun repository(db: PayslipDatabase) = RoomPart2Repository(db.part2Dao(), dispatcher = Dispatchers.Unconfined)

    private fun dueModel(repo: Part2Repository): DueViewModel {
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticPart2Templates.bundle())), FakeCrashReporter(), Dispatchers.Unconfined)
        guide.load()
        val drafts = Part2DraftsViewModel(guide, repo, Dispatchers.Unconfined) { india }
        return DueViewModel(repo, drafts, ReminderPlanner { india }, { now }, Dispatchers.Unconfined)
    }

    private suspend fun DueViewModel.until(check: (DueUiState) -> Boolean): DueUiState = withTimeout(10_000) { state.first(check) }

    private fun leave() =
        Part2Draft(
            id = "d1",
            event =
                LeaveEventInput(
                    listOf(
                        OccurrenceRequest(
                            "GTDAL",
                            mapOf(
                                "leave_from" to SlotValue.DateValue(CalendarDate(2026, 10, 5)),
                                "leave_to" to SlotValue.DateValue(CalendarDate(2026, 10, 9)),
                                "year" to SlotValue.NumberValue(2026),
                            ),
                        ),
                    ),
                ),
            templateRevs = emptyMap(),
            createdAt = 1,
            updatedAt = 2,
            printed = PrintedSnapshot(printedAt, emptyMap(), emptyList()),
        )

    @Test
    fun theListFollowsRealStorageThroughPublishingAndASurvivingDismissal() =
        runBlocking {
            val repo = repository(open())
            repo.saveDraft(leave())
            val model = dueModel(repo)

            val first = model.until { it.due.size == 2 }
            assertEquals(setOf(Part2DueStrings.action(ReminderKind.RETURN, null), Part2DueStrings.action(ReminderKind.ADD_ORDER, null)), first.due.map { it.action }.toSet())
            assertEquals(setOf("return", "add-order"), first.due.map { it.task }.toSet())

            repo.saveOrder(SyntheticPart2Drafts.order("o1", "d1", "A/12", CalendarDate(2026, 6, 20)))
            model.until { it.due.size == 1 }

            model.dismiss("d1", "return")
            val dismissed = model.until { it.due.isEmpty() }
            assertEquals(1, dismissed.upcoming.size, "only the year end is left, coming up")

            val again = repository(reopen())
            assertEquals(setOf("return"), again.draft("d1")!!.dismissed)
            val reopened = dueModel(again).until { it.upcoming.size == 1 }
            assertTrue(reopened.due.isEmpty(), "closing and opening the app keeps the dismissal")
        }

    @Test
    fun theCardCountsWhatTheRealRepositoryHoldsAndLetsGoOnNotNow() =
        runBlocking {
            val repo = repository(open())
            repo.saveDraft(leave())
            val model = dueModel(repo)
            val card = DueCardModel(model)
            model.until { it.due.size == 2 }

            card.onImportCompleted()
            assertEquals(2, withTimeout(10_000) { card.count.first { it > 0 } })

            card.notNow()
            assertEquals(0, withTimeout(10_000) { card.count.first { it == 0 } })
        }
}
