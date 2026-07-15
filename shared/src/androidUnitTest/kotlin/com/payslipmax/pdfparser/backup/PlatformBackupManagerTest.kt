package com.payslipmax.pdfparser.backup

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Robolectric coverage for the Android [PlatformBackupManager] actual — the real Context-dependent
 * file-swap glue over the shared AES-256 [com.payslipmax.pdfparser.crypto.CryptoHelper]. The database
 * is built only to satisfy the constructor (its `close()` runs during restore); it is never queried,
 * so the bundled SQLite driver is not exercised here.
 */
@RunWith(RobolectricTestRunner::class)
class PlatformBackupManagerTest {
    private lateinit var context: Context
    private lateinit var database: PayslipDatabase
    private lateinit var manager: PlatformBackupManager
    private lateinit var dbFile: File
    private lateinit var backupFile: File

    @BeforeTest
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        ContextHolder.context = context
        database =
            getDatabaseBuilder()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        manager = PlatformBackupManager(database)

        dbFile = context.getDatabasePath("payslips.db")
        dbFile.parentFile?.mkdirs()
        backupFile = File(context.getExternalFilesDir(null) ?: context.filesDir, "backup.pcda")
        backupFile.delete()
    }

    @AfterTest
    fun tearDown() {
        try {
            database.close()
        } catch (_: Exception) {
        }
        backupFile.delete()
        ContextHolder.context = null
    }

    @Test
    fun backupThenRestoreRoundTripsTheDatabaseBytes() =
        runTest {
            val original = "PCDA_DB_CONTENT_v10".encodeToByteArray()
            dbFile.writeBytes(original)

            assertTrue(manager.backup("correct-horse").isSuccess)
            assertTrue(backupFile.exists(), "an encrypted backup file must be written")

            // Corrupt the live DB, then restore from the encrypted backup.
            dbFile.writeBytes("CORRUPTED".encodeToByteArray())
            assertTrue(manager.restore("correct-horse").isSuccess)

            assertContentEquals(original, dbFile.readBytes(), "restore must reproduce the original bytes")
        }

    @Test
    fun restoreWithWrongPasswordFails() =
        runTest {
            dbFile.writeBytes("PCDA_DB_CONTENT".encodeToByteArray())
            assertTrue(manager.backup("right-password").isSuccess)

            assertTrue(manager.restore("wrong-password").isFailure, "decryption with the wrong password must fail")
        }

    @Test
    fun restoreWithNoBackupFileFails() =
        runTest {
            backupFile.delete()
            assertTrue(manager.restore("any-password").isFailure, "restore must fail when no backup exists")
        }
}
