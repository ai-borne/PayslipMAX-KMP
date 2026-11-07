package com.payslipmax.pdfparser.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.cinterop.toKString
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.posix.getenv
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val V15 = 15
private const val V16 = 16
private const val DATABASE_CLASS_DIR = "com.payslipmax.pdfparser.database.PayslipDatabase"

/**
 * WHY: payslips exist only on the phone, so a Room upgrade that fails or drops rows loses the officer's only copy. Android has
 * PayslipDatabaseUpgradeTest; iOS runs the same migrations on the NATIVE sqlite driver and Kotlin/Native Room runtime, which had no
 * test of its own. Every exported schema version must upgrade to head, and rows must come through the oldest and the most recent hops.
 *
 * The exported schema JSONs are found through PAYSLIP_SCHEMA_DIR (the schemas root, which Room appends the database class folder to),
 * set by shared/build.gradle.kts for the simulator test task. Head and oldest versions come from those files, as in the Android test,
 * so a version bump needs no edit here. A missing variable FAILS the test rather than skipping it, so the gate cannot go quiet.
 */
class IosPayslipDatabaseUpgradeTest {
    // One file per test, deleted before every createDatabase: a file left at head would otherwise be read as a DOWNGRADE by the next old version.
    private val dbPath = NSTemporaryDirectory().trimEnd('/') + "/upgrade-" + NSUUID().UUIDString + ".db"
    private val helper =
        MigrationTestHelper(
            schemaDirectoryPath = schemaDir(),
            fileName = dbPath,
            driver = BundledSQLiteDriver(),
            databaseClass = PayslipDatabase::class,
            databaseFactory = { PayslipDatabaseConstructor.initialize() },
        )

    private val versions = exportedVersions()
    private val head = versions.max()
    private val oldest = versions.min()

    @AfterTest
    fun deleteDatabase() = removeDatabaseFiles()

    private fun removeDatabaseFiles() = listOf("", "-wal", "-shm", "-journal").forEach { NSFileManager.defaultManager.removeItemAtPath(dbPath + it, null) }

    private fun fresh(version: Int): SQLiteConnection {
        removeDatabaseFiles()
        return helper.createDatabase(version)
    }

    @Test
    fun everyShippedSchemaVersionUpgradesToHeadOnTheNativeDriver() =
        runTest {
            assertEquals((oldest..head).toList(), versions, "exported schema versions must be contiguous")
            (oldest until head).forEach { version ->
                fresh(version).close()
                helper.runMigrationsAndValidate(head).close()
            }
        }

    @Test
    fun aPayslipSurvivesTheWholeChainFromTheOldestVersion() =
        runTest {
            fresh(oldest).use { it.insertPayslip() }

            helper.runMigrationsAndValidate(head).use { db ->
                assertEquals(1, db.count("encrypted_payslips"), "the payslip must survive oldest -> head on iOS")
            }
        }

    @Test
    fun theLastHopKeepsOrdersAndPagesAndDropsTheFrozenDraftsTable() =
        runTest {
            fresh(V15).use { db ->
                db.insertPayslip()
                db.execSQL("INSERT INTO part2_drafts (id, ciphertext) VALUES ('d1', x'0102')")
                db.execSQL("INSERT INTO part2_orders (id, ciphertext) VALUES ('o1', x'0A1B2C3D')")
                db.execSQL("INSERT INTO part2_pages (id, orderId, position, ciphertext) VALUES ('p1', 'o1', 0, x'CAFE')")
            }

            helper.runMigrationsAndValidate(V16).use { db ->
                assertEquals(1, db.count("encrypted_payslips"))
                assertEquals(1, db.count("part2_orders"), "the scanned order survives")
                assertEquals(1, db.count("part2_pages"), "its page survives")
                assertEquals(0, db.count("part2_entries"), "the new entries table starts empty")
                assertEquals(0, db.count("sqlite_master WHERE type = 'table' AND name = 'part2_drafts'"), "drafts are dropped")
            }
        }

    private fun SQLiteConnection.insertPayslip() =
        execSQL(
            "INSERT INTO encrypted_payslips (dateStr, year, monthNum, monthName, ciphertext) VALUES ('2026-01', 2026, 1, 'January', 'cafe')",
        )

    private fun SQLiteConnection.count(from: String): Int =
        prepare("SELECT COUNT(*) FROM $from").use {
            assertTrue(it.step())
            it.getLong(0).toInt()
        }
}

private fun schemaDir(): String =
    getenv("PAYSLIP_SCHEMA_DIR")?.toKString()
        ?: error("PAYSLIP_SCHEMA_DIR is not set: the simulator test task must pass the exported Room schema root (see shared/build.gradle.kts)")

private fun exportedVersions(): List<Int> =
    (NSFileManager.defaultManager.contentsOfDirectoryAtPath("${schemaDir()}/$DATABASE_CLASS_DIR", null) as List<*>?)
        .orEmpty()
        .map { it as String }
        .filter { it.endsWith(".json") }
        .map { it.removeSuffix(".json").toInt() }
        .sorted()
