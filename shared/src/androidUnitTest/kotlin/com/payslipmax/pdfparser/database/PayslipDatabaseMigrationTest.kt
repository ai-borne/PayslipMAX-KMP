package com.payslipmax.pdfparser.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TEST_DB = "migration-test.db"

/**
 * Exercises the two migrations that drop schema (a column, a table) rather than only add to it —
 * the shape of change most likely to silently diverge from a shipped version's exported schema.
 * See PayslipDatabase.kt's v10->v11 AutoMigration and the crash it once caused: a shipped version's
 * schema JSON was rewritten in place without bumping the version, so devices already on that
 * version skipped migration entirely and failed Room's identity-hash validation on open.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PayslipDatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PayslipDatabase::class.java,
        )

    @Test
    fun `migrate5To6 deletes geminiApiKey column from app_settings`() {
        helper.createDatabase(TEST_DB, 5).close()
        helper.runMigrationsAndValidate(TEST_DB, 6, true)
    }

    @Test
    fun `migrate10To11 deletes ai_insight_reports table`() {
        helper.createDatabase(TEST_DB, 10).close()
        helper.runMigrationsAndValidate(TEST_DB, 11, true)
    }
}
