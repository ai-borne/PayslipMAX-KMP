package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.database.GuideNoteEntity
import com.payslipmax.pdfparser.database.toEntity
import com.payslipmax.pdfparser.guide.domain.GUIDE_NOTE_MAX_CHARS
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The behaviour every [GuideNotesRepository] must have, run against the in-memory fake the UI tests use and against the
 * Room-backed one (over the fake DAO, so only the encryption and mapping are real). The Room file itself is covered by
 * `GuideNotesRoomTest` (JVM) and `GuideNotesRoomIosTest` (Kotlin/Native).
 */
abstract class GuideNotesRepositoryContractTest {
    abstract fun repository(clock: () -> Long): GuideNotesRepository

    private var now = 100L
    private val repo by lazy { repository { now } }

    private suspend fun notes() = repo.observe().first().notes

    @Test
    fun aFreshInstallHasNoNotes() =
        runTest {
            assertTrue(notes().isEmpty())
            assertEquals(0, repo.observe().first().unreadable)
        }

    @Test
    fun aSavedNoteComesBackTrimmedAndStampedWithTheTimeAndTheCardRevision() =
        runTest {
            assertEquals(GuideNoteSaveResult.SAVED, repo.save("RB-TD-001", "  check with unit  ", "a1b2c3d4"))

            val note = notes().single()
            assertEquals("check with unit", note.text)
            assertEquals("a1b2c3d4", note.cardRev)
            assertEquals(100L, note.updatedAt)
        }

    @Test
    fun savingAgainReplacesTheNoteOnThatCardAndKeepsTheOthers() =
        runTest {
            repo.save("RB-TD-001", "first", "r1")
            repo.save("RB-TD-002", "other card", "r1")
            now = 200L

            repo.save("RB-TD-001", "second", "r2")

            val byCard = notes().associateBy { it.cardId }
            assertEquals(setOf("RB-TD-001", "RB-TD-002"), byCard.keys)
            assertEquals("second", byCard.getValue("RB-TD-001").text)
            assertEquals(200L, byCard.getValue("RB-TD-001").updatedAt)
            assertEquals("other card", byCard.getValue("RB-TD-002").text)
        }

    @Test
    fun blankTextDeletesTheNoteSoClearingTheEditorRemovesIt() =
        runTest {
            repo.save("RB-TD-001", "something", "r1")

            assertEquals(GuideNoteSaveResult.DELETED, repo.save("RB-TD-001", "   ", "r1"))

            assertTrue(notes().isEmpty())
        }

    @Test
    fun blankTextForACardWithNoNoteIsHarmless() =
        runTest {
            assertEquals(GuideNoteSaveResult.DELETED, repo.save("RB-TD-009", "", "r1"))

            assertTrue(notes().isEmpty())
        }

    @Test
    fun anIdOrRevisionThatIsNotAPlainTokenIsRejectedAndNothingIsTouched() =
        runTest {
            repo.save("RB-TD-001", "keep me", "r1")

            assertEquals(GuideNoteSaveResult.REJECTED, repo.save("<b>", "x", "r1"))
            assertEquals(GuideNoteSaveResult.REJECTED, repo.save("RB-TD-001", "x", "<script>"))
            assertEquals(GuideNoteSaveResult.REJECTED, repo.save("../etc", "", "r1"), "even an erase request on a bad id changes nothing")

            assertEquals("keep me", notes().single().text)
        }

    @Test
    fun theCapIsEnforcedOnSaveWhateverTheCallerSends() =
        runTest {
            repo.save("RB-TD-001", "z".repeat(GUIDE_NOTE_MAX_CHARS + 700), "r1")

            assertEquals(GUIDE_NOTE_MAX_CHARS, notes().single().text.length)
        }

    @Test
    fun deleteRemovesOnlyThatCardsNote() =
        runTest {
            repo.save("RB-TD-001", "a", "r1")
            repo.save("RB-TD-002", "b", "r1")

            repo.delete("RB-TD-001")
            repo.delete("RB-NEVER-HAD-ONE")

            assertEquals(listOf("RB-TD-002"), notes().map { it.cardId })
        }

    @Test
    fun observersSeeEachChangeWithoutAskingAgain() =
        runTest {
            val flow = repo.observe()
            assertTrue(flow.first().notes.isEmpty())

            repo.save("RB-TD-001", "a", "r1")

            assertEquals(1, flow.first().notes.size)
        }
}

class FakeGuideNotesRepositoryContractTest : GuideNotesRepositoryContractTest() {
    override fun repository(clock: () -> Long): GuideNotesRepository = FakeGuideNotesRepository(clock)
}

class RoomGuideNotesRepositoryContractTest : GuideNotesRepositoryContractTest() {
    override fun repository(clock: () -> Long): GuideNotesRepository =
        RoomGuideNotesRepository(FakePayslipDao(), key = { KEY }, clock = clock, dispatcher = Dispatchers.Unconfined)

    @Test
    fun anUnreadableRowIsSkippedAndCountedNotShownAndNotDeleted() =
        runTest {
            val dao = FakePayslipDao()
            val repo = RoomGuideNotesRepository(dao, key = { KEY }, clock = { 1L }, dispatcher = Dispatchers.Unconfined)
            repo.save("RB-TD-001", "readable", "r1")
            dao.insertGuideNote(GuideNoteEntity("RB-TD-002", "deadbeef"))
            dao.insertGuideNote(assertNotNull(GuideNote.of("RB-TD-003", "other device", "r1", 1L)).toEntity("a different device key"))

            val seen = repo.observe().first()

            assertEquals(listOf("RB-TD-001"), seen.notes.map { it.cardId })
            assertEquals(2, seen.unreadable)
            assertEquals(3, dao.getAllGuideNotes().first().size, "an unreadable row is kept, never deleted behind the user's back")
        }

    @Test
    fun theStoredRowHoldsNoPlaintext() =
        runTest {
            val dao = FakePayslipDao()
            val repo = RoomGuideNotesRepository(dao, key = { KEY }, clock = { 1L }, dispatcher = Dispatchers.Unconfined)

            repo.save("RB-TD-001", "very private words", "a1b2c3d4")

            val row = dao.getAllGuideNotes().first().single()
            assertTrue(!row.ciphertext.contains("private") && !row.ciphertext.contains("a1b2c3d4"))
        }

    private companion object {
        const val KEY = "device-key-for-tests"
    }
}
