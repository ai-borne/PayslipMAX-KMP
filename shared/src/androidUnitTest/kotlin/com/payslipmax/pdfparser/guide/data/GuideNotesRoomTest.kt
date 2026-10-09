package com.payslipmax.pdfparser.guide.data

import android.content.Context
import androidx.room.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.getDatabaseBuilder
import com.payslipmax.pdfparser.database.toEntity
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.backupBytes
import com.payslipmax.pdfparser.repository.goodEntity
import com.payslipmax.pdfparser.repository.seedDeviceMonth
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val SECRET = "plain-text-marker-zq9-TPTA-doubt"

/**
 * WHY: the fake DAO has no file and no SQL, so it cannot show the two promises a private note depends on: that what a user
 * wrote is still there after the app is closed and reopened, and that nothing readable is in the database file. These run
 * the production schema in a real SQLite file on the JVM. The Kotlin/Native twin is `GuideNotesRoomIosTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GuideNotesRoomTest {
    private lateinit var context: Context
    private var database: PayslipDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ContextHolder.context = context
    }

    @After
    fun tearDown() {
        database?.close()
        ContextHolder.context = null
    }

    // Production builder (file database), driver swapped only because the bundled native driver cannot load on the host JVM.
    private fun open(): PayslipDatabase = getDatabaseBuilder().setDriver(AndroidSQLiteDriver()).build().also { database = it }

    private fun reopen(): PayslipDatabase {
        database?.close()
        return open()
    }

    private fun repository(db: PayslipDatabase) = RoomGuideNotesRepository(db.payslipDao(), dispatcher = Dispatchers.Unconfined)

    @Test
    fun `a saved note survives closing and reopening the database`() =
        runBlocking {
            repository(open()).save("RB-TD-001", SECRET, "a1b2c3d4")

            val seen = repository(reopen()).observe().first()

            val note = assertNotNull(seen.notes.singleOrNull())
            assertEquals(SECRET, note.text)
            assertEquals("a1b2c3d4", note.cardRev)
            assertEquals(0, seen.unreadable)
        }

    @Test
    fun `the note text is not readable anywhere in the database files`() =
        runBlocking {
            val db = open()
            repository(db).save("RB-TD-001", SECRET, "a1b2c3d4")
            db.close()

            val files = context.getDatabasePath("payslips.db").parentFile!!.listFiles { f -> f.name.startsWith("payslips.db") }!!
            assertTrue(files.isNotEmpty(), "the database file exists, so the scan below is not vacuous")
            files.forEach { file ->
                val bytes = file.readBytes().decodeToString()
                assertFalse(bytes.contains("plain-text-marker"), "${file.name} must not hold the note text")
                assertFalse(bytes.contains("a1b2c3d4"), "${file.name} must not hold the card revision")
            }
            val dbBytes = files.first { it.name == "payslips.db" }.readBytes().decodeToString()
            assertTrue(dbBytes.contains("RB-TD-001"), "the card id is the one plaintext (the row key), as documented")
        }

    @Test
    fun `a note keeps the card it was written for when many are stored`() =
        runBlocking {
            val repo = repository(open())
            repo.save("RB-TD-001", "one", "r")
            repo.save("RB-TD-002", "two", "r")
            repo.delete("RB-TD-001")

            val byCard = repository(reopen()).observe().first().notes.associate { it.cardId to it.text }

            assertEquals(mapOf("RB-TD-002" to "two"), byCard)
        }

    @Test
    fun `a failing notes write rolls the whole restore back and the device keeps its note`() =
        runBlocking {
            val db = open()
            val dao = db.payslipDao()
            dao.seedDeviceMonth("01/2023")
            db.useWriterConnection {
                it.usePrepared(
                    "CREATE TRIGGER fail_note BEFORE INSERT ON guide_notes BEGIN SELECT RAISE(ABORT, 'simulated failure'); END",
                ) { statement -> statement.step() }
            }
            val note = assertNotNull(GuideNote.of("RB-NEW", "from backup", "r", 2L))
            val backup =
                backupBytes(
                    PortableBackup(
                        version = 4,
                        encryptedPayslips = listOf(goodEntity("08/2024")),
                        pdfs = emptyList(),
                        settings = null,
                        guideNotes = listOf(note.toEntity(BACKUP_PASSWORD)),
                    ),
                )

            val result = PayslipBackupService(dao, Dispatchers.Unconfined).restore(backup, BACKUP_PASSWORD, RestoreMode.REPLACE)

            assertTrue(result.isFailure)
            assertEquals(listOf("01/2023"), dao.getAllPayslips().first().map { it.dateStr }, "the payslips were not wiped")
            assertEquals(1, dao.getAllGuideNotes().first().size, "the device's own note is still there")
        }

    @Test
    fun `a backup with notes restores them into the real database`() =
        runBlocking {
            val db = open()
            val dao = db.payslipDao()
            val service = PayslipBackupService(dao, Dispatchers.Unconfined)
            repository(db).save("RB-TD-001", SECRET, "r1")
            val archive = service.export(BACKUP_PASSWORD).getOrThrow()
            dao.clearAllUserData()
            assertTrue(dao.getAllGuideNotes().first().isEmpty())

            service.restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(SECRET, repository(reopen()).observe().first().notes.single().text)
        }
}
