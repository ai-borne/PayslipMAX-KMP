package com.payslipmax.pdfparser.part2.data

import android.content.Context
import androidx.room.useWriterConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.getDatabaseBuilder
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.seedDeviceMonth
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the fake DAO has no file and no SQL, so it cannot show what the officer relies on: a entry, a scanned order and its pages are
 * still there after the app is closed and reopened, nothing readable is in the database file, the caps hold inside real transactions,
 * and a restore that fails halfway leaves the device exactly as it was. These run the production schema in a real SQLite file on the
 * JVM. The Kotlin/Native twin is `Part2RoomIosTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Part2RoomTest {
    private lateinit var context: Context
    private var database: PayslipDatabase? = null
    private val jpeg = SyntheticBank.jpeg(20_000, 3)

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

    private fun open(): PayslipDatabase = getDatabaseBuilder().setDriver(AndroidSQLiteDriver()).build().also { database = it }

    private fun reopen(): PayslipDatabase {
        database?.close()
        return open()
    }

    private fun repository(db: PayslipDatabase) = RoomPart2Repository(db.part2Dao(), dispatcher = Dispatchers.Unconfined)

    private fun service(db: PayslipDatabase) = PayslipBackupService(db.payslipDao(), Dispatchers.Unconfined, part2Backup = Part2Backup(db.part2Dao()))

    private suspend fun fill(repo: RoomPart2Repository): String {
        repo.saveEntry(SyntheticBank.entry())
        repo.saveOrder(SyntheticBank.order())
        return assertNotNull(repo.addPage("order-1", jpeg).pageId)
    }

    @Test
    fun `a entry an order and a page survive closing and reopening the database`() =
        runBlocking {
            val pageId = fill(repository(open()))

            val repo = repository(reopen())

            assertEquals(SyntheticBank.entry(), repo.entry("entry-1"))
            assertEquals(SyntheticBank.order(), repo.observeOrders().first().items.single())
            assertContentEquals(jpeg, repo.readPage(pageId) { it.copyOf() })
            assertEquals(listOf(pageId), repo.pages("order-1").map { it.id })
        }

    @Test
    fun `nothing readable is in the database files except the ids`() =
        runBlocking {
            val db = open()
            fill(repository(db))
            db.close()

            val files = context.getDatabasePath("payslips.db").parentFile!!.listFiles { f -> f.name.startsWith("payslips.db") }!!
            assertTrue(files.isNotEmpty(), "the database file exists, so the scan below is not vacuous")
            files.forEach { file ->
                val bytes = file.readBytes()
                val text = bytes.decodeToString()
                for (secret in listOf(SyntheticBank.MARKER_NOTE, SyntheticBank.MARKER_ORDER_NO, "SYNCODE", "leave_from", "a1b2c3d4")) {
                    assertFalse(text.contains(secret), "${file.name} must not hold $secret")
                }
                val chunk = jpeg.copyOfRange(500, 532).toList()
                assertFalse(bytes.toList().windowed(32).any { it == chunk }, "${file.name} must not hold a run of the plain page")
            }
            assertTrue(files.first { it.name == "payslips.db" }.readBytes().decodeToString().contains("order-1"), "ids are the plaintext, as documented")
        }

    @Test
    fun `deleting a entry keeps its order and deleting an order deletes its pages in real SQL`() =
        runBlocking {
            val db = open()
            val repo = repository(db)
            val pageId = fill(repo)

            repo.deleteEntry("entry-1")
            assertEquals(1, repo.observeOrders().first().items.size)
            assertEquals(1, repo.pages("order-1").size)

            repo.deleteOrder("order-1")
            assertNull(repo.readPage(pageId) { it })
            assertEquals(0, db.part2Dao().allPageRefs().size)
            assertEquals(0L, repo.vaultBytes())
        }

    @Test
    fun `the page cap holds inside the real transaction`() =
        runBlocking {
            val repo = repository(open())
            repo.saveOrder(SyntheticBank.order())
            repeat(Part2Limits.PAGES_PER_ORDER_MAX) { assertEquals(Part2WriteResult.SAVED, repo.addPage("order-1", SyntheticBank.jpeg(200, it)).result) }

            assertEquals(Part2WriteResult.TOO_MANY, repo.addPage("order-1", SyntheticBank.jpeg(200)).result)
            assertEquals(Part2Limits.PAGES_PER_ORDER_MAX, repository(reopen()).pages("order-1").size)
        }

    @Test
    fun `an export restores into another database with pages intact and clear all data empties the three tables`() =
        runBlocking {
            val db = open()
            val pageId = fill(repository(db))
            val archive = service(db).export(BACKUP_PASSWORD).getOrThrow()
            db.payslipDao().clearAllUserData()
            assertTrue(db.part2Dao().allEntries().isEmpty() && db.part2Dao().allOrders().isEmpty() && db.part2Dao().allPageRefs().isEmpty())

            service(db).restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            val repo = repository(reopen())
            assertEquals(SyntheticBank.entry(), repo.entry("entry-1"))
            assertContentEquals(jpeg, repo.readPage(pageId) { it.copyOf() })
        }

    @Test
    fun `a merge restore layers the backup on top of what the device holds`() =
        runBlocking {
            val db = open()
            val repo = repository(db)
            fill(repo)
            val archive = service(db).export(BACKUP_PASSWORD).getOrThrow()
            repo.saveEntry(SyntheticBank.entry("device-only"))
            repo.saveEntry(SyntheticBank.entry(updatedAt = 1)) // older than the backup's copy

            service(db).restore(archive, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            assertEquals(setOf("entry-1", "device-only"), repository(reopen()).observeEntries().first().items.map { it.id }.toSet())
            assertEquals(SyntheticBank.entry().updatedAt, repository(database!!).entry("entry-1")!!.updatedAt, "the newer backup copy won")
        }

    @Test
    fun `a failing pages write rolls the whole restore back and the device keeps everything`() =
        runBlocking {
            val db = open()
            val repo = repository(db)
            fill(repo)
            val archive = service(db).export(BACKUP_PASSWORD).getOrThrow()
            db.payslipDao().seedDeviceMonth("01/2023")
            repo.saveEntry(SyntheticBank.entry("device-only"))
            db.useWriterConnection {
                it.usePrepared("CREATE TRIGGER fail_page BEFORE INSERT ON part2_pages BEGIN SELECT RAISE(ABORT, 'simulated failure'); END") { s -> s.step() }
            }

            val result = service(db).restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE)

            assertTrue(result.isFailure)
            assertEquals(listOf("01/2023"), db.payslipDao().getAllPayslips().first().map { it.dateStr }, "the payslips were not wiped")
            assertEquals(setOf("entry-1", "device-only"), db.part2Dao().allEntries().map { it.id }.toSet(), "the device's own entries are still there")
            assertEquals(1, db.part2Dao().allPageRefs().size)
        }

    @Test
    fun `two writes racing at the ceiling cannot both pass the count check`() =
        runBlocking {
            val dao = open().part2Dao()
            val results =
                (1..8).map { n -> async(Dispatchers.Default) { dao.saveEntryWithin(Part2EntryEntity("racer-$n", byteArrayOf(1)), 1) } }.awaitAll()

            assertEquals(1, results.count { it == Part2WriteResult.SAVED }, "exactly one racer wins the single slot")
            assertEquals(1, dao.entryCount())
        }
}
