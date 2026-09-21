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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val PRODUCTION_DB = "payslips.db"
private const val SCHEMA_DIR = "schemas/com.payslipmax.pdfparser.database.PayslipDatabase"
private const val FUTURE_VERSION = 99

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
