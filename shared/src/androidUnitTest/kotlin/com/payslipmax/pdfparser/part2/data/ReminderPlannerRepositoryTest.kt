package com.payslipmax.pdfparser.part2.data

import android.content.Context
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.getDatabaseBuilder
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderPlan
import com.payslipmax.pdfparser.part2.domain.ReminderTestBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * WHY: the planner's unit tests hand it plain objects. This runs it on what the production schema actually returns, encrypted and decrypted,
 * so a print record, a request value, a quoted order or a draft-to-order link lost on the way through storage fails here: the Due list is
 * only as good as what the repository gives back, including after the app is closed and opened again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReminderPlannerRepositoryTest : ReminderTestBase() {
    private var database: PayslipDatabase? = null
    private val printed = at(date(2026, 9, 20))
    private val now = at(date(2026, 10, 2))
    private val orderDate = date(2026, 6, 20)

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

    private suspend fun planFrom(repo: RoomPart2Repository): ReminderPlan = planner.plan(repo.observeDrafts().first().items, repo.observeOrders().first().items, now)

    private fun ReminderPlan.kindsOf(id: String) = all().filter { it.draftId == id }.map { it.kind }.toSet()

    @Test
    fun theDueListFollowsWhatTheRealRepositoryHoldsThroughPublishingAndCancelling() =
        runBlocking {
            val repo = repository(open())
            assertEquals(Part2WriteResult.SAVED, repo.saveDraft(draft("d1", leave(date(2026, 10, 5), date(2026, 10, 9)), printedAt = printed)))
            assertEquals(setOf(ReminderKind.RETURN, ReminderKind.ADD_ORDER, ReminderKind.YEAR_END), planFrom(repo).kindsOf("d1"))

            assertEquals(Part2WriteResult.SAVED, repo.saveOrder(order("o1", "d1", "0123/26", orderDate)))
            assertEquals(setOf(ReminderKind.RETURN, ReminderKind.YEAR_END), planFrom(repo).kindsOf("d1"))

            assertEquals(Part2WriteResult.SAVED, repo.saveDraft(cancelSlip("c1", "CANCEL", "0123/26", orderDate, printed + 1)))
            val cancelled = planFrom(repo)
            assertEquals(emptySet(), cancelled.kindsOf("d1"))
            assertEquals(setOf(ReminderKind.ADD_ORDER), cancelled.kindsOf("c1"))
        }

    @Test
    fun theSamePlanComesBackAfterTheAppIsClosedAndOpenedAgain() =
        runBlocking {
            val first = repository(open())
            first.saveDraft(draft("d1", leave(date(2026, 10, 5), date(2026, 10, 9)), printedAt = printed))
            first.saveDraft(draft("r1", request("REJOINAL", on("rejoined_on", date(2026, 10, 10)))))
            first.saveOrder(order("o1", "d1", "0123/26", orderDate))
            val before = planFrom(first)

            val again = planFrom(repository(reopen()))
            assertEquals(before, again)
            // the rejoining draft answered the return, and the published order ended the add-the-order series: only the year end is left
            assertEquals(setOf(ReminderKind.YEAR_END), again.kindsOf("d1"))
        }
}
