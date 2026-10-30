package com.payslipmax.pdfparser.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.payslipmax.pdfparser.crypto.ContextHolder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val PRODUCTION_DB = "payslips.db"
private const val SCHEMA_DIR = "schemas/com.payslipmax.pdfparser.database.PayslipDatabase"
private const val FUTURE_VERSION = 99
private const val V12 = 12
private const val V13 = 13
private const val V14 = 14
private const val V15 = 15
private const val V16 = 16

/**
 * WHY: user payslips exist only in this database (there is no server copy), so the database must
 * never be silently dropped and recreated. Room's destructive-migration fallback did exactly that
 * for any version it could not migrate; these tests pin the replacement policy — every shipped
 * version upgrades to head with its rows intact, and anything unrecognised fails loudly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PayslipDatabaseUpgradeTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PayslipDatabase::class.java,
        )

    @After
    fun clearContext() {
        ContextHolder.context = null
    }

    @Test
    fun `a database with an unrecognised version fails loudly instead of being wiped`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ContextHolder.context = context
        helper.createDatabase(PRODUCTION_DB, headVersion()).use { db ->
            db.insertPayslip("2026-01")
            db.execSQL("PRAGMA user_version = $FUTURE_VERSION")
        }

        // Production builder, so the migration policy under test is the one that ships. Only the
        // driver is swapped: the bundled native driver cannot load on the host JVM.
        val database = getDatabaseBuilder().setDriver(AndroidSQLiteDriver()).build()
        try {
            assertFailsWith<IllegalStateException> {
                runBlocking { database.payslipDao().getPayslipByDate("2026-01") }
            }
        } finally {
            // Room's close() retries the open it just failed, so it throws the same error again.
            runCatching { database.close() }
        }

        assertEquals(1, payslipRowCount(context.getDatabasePath(PRODUCTION_DB)), "payslip must survive the failed open")
    }

    @Test
    fun `every exported schema version upgrades to the current version`() {
        val versions = exportedVersions()
        val head = versions.max()
        assertEquals((versions.min()..head).toList(), versions, "exported schema versions must be contiguous")

        versions.filter { it < head }.forEach { version ->
            val name = "upgrade-from-$version.db"
            helper.createDatabase(name, version).close()
            helper.runMigrationsAndValidate(name, head, true).close()
        }
    }

    @Test
    fun `payslips survive an upgrade from the oldest exported version to the current one`() {
        val oldest = exportedVersions().min()
        helper.createDatabase(PRODUCTION_DB, oldest).use { it.insertPayslip("2026-01") }

        helper.runMigrationsAndValidate(PRODUCTION_DB, headVersion(), true).use { db ->
            db.query("SELECT dateStr, ciphertext FROM encrypted_payslips").use { cursor ->
                assertTrue(cursor.moveToFirst(), "migrated database lost the payslip row")
                assertEquals("2026-01", cursor.getString(0))
                assertEquals("cafe", cursor.getString(1))
                assertEquals(1, cursor.count)
            }
        }
    }

    // WHY: a letter the officer deletes must stay deleted across re-audits, which needs a record of the
    // deletion (Phase 10 of the post-Phase-8 work). The table arrives with the upgrade, and the officer's
    // existing letters must come through it untouched.
    @Test
    fun `the v12 database gains the dismissed drafts table and keeps its letters`() {
        helper.createDatabase(PRODUCTION_DB, V12).use {
            it.execSQL(
                "INSERT INTO representation_drafts (id, disputeMonth, disputeType, recipient, subject, bodyText, createdAt) " +
                    "VALUES ('d1', '03/2018', 'TPTA_ENTITLEMENT', 'PCDA', 's', 'b', 1)",
            )
        }

        helper.runMigrationsAndValidate(PRODUCTION_DB, headVersion(), true).use { db ->
            db.query("SELECT id FROM representation_drafts").use { assertEquals(1, it.count, "the letter survives the upgrade") }
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'dismissed_drafts'").use {
                assertEquals(1, it.count, "dismissed_drafts table exists after the upgrade")
            }
        }
    }

    // WHY: personal Guide notes (M6) arrive with the v14 table. The officer's payslips and corrections are the only copy of
    // their data, so they must come through the upgrade untouched, and the new table must start empty and usable.
    @Test
    fun `the v13 database gains the guide notes table and keeps its payslips and corrections`() {
        helper.createDatabase(PRODUCTION_DB, V13).use {
            it.insertPayslip("2026-01")
            it.execSQL("INSERT INTO payslip_corrections (dateStr, ciphertext) VALUES ('2026-01', 'beef')")
        }

        helper.runMigrationsAndValidate(PRODUCTION_DB, headVersion(), true).use { db ->
            db.query("SELECT ciphertext FROM encrypted_payslips").use { assertEquals(1, it.count, "the payslip survives the upgrade") }
            db.query("SELECT ciphertext FROM payslip_corrections").use { assertEquals(1, it.count, "the correction survives the upgrade") }
            db.query("SELECT cardId, ciphertext FROM guide_notes").use { assertEquals(0, it.count, "the new table starts empty") }
            db.execSQL("INSERT INTO guide_notes (cardId, ciphertext) VALUES ('RB-TD-001', 'cafe')")
            db.query("SELECT cardId FROM guide_notes").use { assertEquals(1, it.count) }
        }
    }

    // WHY: the owner's phone holds real payslips at schema 15 plus Part II orders and scanned pages from the frozen feature/part2-orders
    // branch. Schema 16 replaces the drafts table with the Orders Bank entries table; the scanned orders are the officer's photo library, so
    // they and their pages must come through byte for byte, and nothing else may be touched.
    @Test
    fun `the old v15 database becomes v16 keeping orders and pages byte for byte, dropping drafts and gaining entries`() {
        val orderBytes = byteArrayOf(0x0A, 0x1B, 0x2C, 0x3D)
        val pageBytes = ByteArray(2048) { (it * 7).toByte() }
        helper.createDatabase(PRODUCTION_DB, V15).use { db ->
            db.insertPayslip("2026-01")
            db.execSQL("INSERT INTO part2_drafts (id, ciphertext) VALUES ('d1', x'0102')")
            db.execSQL("INSERT INTO part2_orders (id, ciphertext) VALUES ('o1', ?)", arrayOf<Any>(orderBytes))
            db.execSQL("INSERT INTO part2_pages (id, orderId, position, ciphertext) VALUES ('p1', 'o1', 0, ?)", arrayOf<Any>(pageBytes))
        }

        helper.runMigrationsAndValidate(PRODUCTION_DB, V16, true).use { db ->
            db.query("SELECT ciphertext FROM encrypted_payslips").use { assertEquals(1, it.count, "the payslip survives") }
            db.query("SELECT ciphertext FROM part2_orders WHERE id = 'o1'").use {
                assertTrue(it.moveToFirst(), "the order survives")
                assertContentEquals(orderBytes, it.getBlob(0))
            }
            db.query("SELECT orderId, position, ciphertext FROM part2_pages WHERE id = 'p1'").use {
                assertTrue(it.moveToFirst(), "the page survives")
                assertEquals("o1", it.getString(0))
                assertEquals(0, it.getInt(1))
                assertContentEquals(pageBytes, it.getBlob(2))
            }
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'part2_drafts'").use { assertEquals(0, it.count, "drafts are dropped") }
            db.query("SELECT id FROM part2_entries").use { assertEquals(0, it.count, "the entries table starts empty") }
            db.execSQL("INSERT INTO part2_entries (id, ciphertext) VALUES ('e1', x'CAFE')")
            db.query("SELECT id FROM part2_entries").use { assertEquals(1, it.count) }
        }
    }

    // WHY: a phone that never ran the frozen branch is still at schema 14. It must reach 16 through 15 with its payslips intact and the
    // three Part II tables present and empty, so a first install of the Orders Bank needs no special case.
    @Test
    fun `the v14 database reaches v16 with its payslips and empty Part II tables`() {
        helper.createDatabase(PRODUCTION_DB, V14).use { it.insertPayslip("2026-01") }

        helper.runMigrationsAndValidate(PRODUCTION_DB, V16, true).use { db ->
            db.query("SELECT ciphertext FROM encrypted_payslips").use { assertEquals(1, it.count, "the payslip survives") }
            for (table in listOf("part2_entries", "part2_orders", "part2_pages")) {
                db.query("SELECT COUNT(*) FROM $table").use {
                    assertTrue(it.moveToFirst())
                    assertEquals(0, it.getInt(0), "$table starts empty")
                }
            }
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'part2_drafts'").use { assertEquals(0, it.count) }
        }
    }

    private fun SupportSQLiteDatabase.insertPayslip(dateStr: String) =
        execSQL(
            "INSERT INTO encrypted_payslips (dateStr, year, monthNum, monthName, ciphertext) " +
                "VALUES ('$dateStr', 2026, 1, 'January', 'cafe')",
        )

    private fun payslipRowCount(dbFile: File): Int =
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM encrypted_payslips", null).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
        }

    /** Head is the newest exported schema; the build regenerates it whenever `@Database.version` changes. */
    private fun headVersion(): Int = exportedVersions().max()

    private fun exportedVersions(): List<Int> =
        File(SCHEMA_DIR)
            .listFiles { file -> file.extension == "json" }
            .orEmpty()
            .map { it.nameWithoutExtension.toInt() }
            .sorted()
}
