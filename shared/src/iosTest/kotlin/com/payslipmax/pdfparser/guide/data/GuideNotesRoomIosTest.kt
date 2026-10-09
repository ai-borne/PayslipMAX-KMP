package com.payslipmax.pdfparser.guide.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.GuideNoteEntity
import com.payslipmax.pdfparser.database.GuideNoteUnreadableException
import com.payslipmax.pdfparser.database.PayslipDatabase
import com.payslipmax.pdfparser.database.PayslipDatabaseConstructor
import com.payslipmax.pdfparser.database.hexToByteArray
import com.payslipmax.pdfparser.database.toEntity
import com.payslipmax.pdfparser.database.toHex
import com.payslipmax.pdfparser.database.toNote
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val SECRET = "plain-text-marker-zq9-TPTA-doubt"

/**
 * WHY: the JVM test (`GuideNotesRoomTest`) cannot show that notes work on Kotlin/Native, where the encryption key comes from
 * the iOS Keychain, AES-GCM runs through CommonCrypto and Room uses the bundled SQLite driver. This runs the production schema
 * in an in-memory database on the simulator with the real Keychain key: a note round-trips, is not readable in its row, a
 * wrong key or a tampered row is refused, and a backup carries it to a fresh database.
 */
class GuideNotesRoomIosTest {
    private lateinit var database: PayslipDatabase
    private lateinit var repository: RoomGuideNotesRepository

    @BeforeTest
    fun setUp() {
        database = newDatabase()
        repository = RoomGuideNotesRepository(database.payslipDao(), dispatcher = Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() = database.close()

    private fun newDatabase(): PayslipDatabase =
        Room
            .inMemoryDatabaseBuilder<PayslipDatabase>(factory = { PayslipDatabaseConstructor.initialize() })
            .setDriver(BundledSQLiteDriver())
            .build()

    @Test
    fun aNoteWrittenThroughTheRepositoryComesBackUnderTheKeychainKey() =
        runTest {
            repository.save("RB-TD-001", SECRET, "a1b2c3d4")

            val note = assertNotNull(repository.observe().first().notes.singleOrNull())
            assertEquals(SECRET, note.text)
            assertEquals("a1b2c3d4", note.cardRev)
        }

    @Test
    fun theStoredRowHoldsNoPlaintext() =
        runTest {
            repository.save("RB-TD-001", SECRET, "a1b2c3d4")

            val row = database.payslipDao().getAllGuideNotes().first().single()

            assertFalse(row.ciphertext.contains("plain-text-marker") || row.ciphertext.contains("a1b2c3d4"))
        }

    @Test
    fun aWrongKeyAndATamperedRowAreRefused() {
        val note = assertNotNull(GuideNote.of("RB-TD-001", SECRET, "r1", 1L))
        val row = note.toEntity()
        assertFailsWith<GuideNoteUnreadableException> { row.toNote("not the keychain key") }

        val bytes = row.ciphertext.hexToByteArray()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()
        assertFailsWith<GuideNoteUnreadableException> { GuideNoteEntity("RB-TD-001", bytes.toHex()).toNote() }
    }

    @Test
    fun deleteAndBlankSaveRemoveTheRow() =
        runTest {
            val dao = database.payslipDao()
            repository.save("RB-TD-001", "a", "r1")
            repository.save("RB-TD-002", "b", "r1")

            repository.delete("RB-TD-001")
            repository.save("RB-TD-002", "  ", "r1")

            assertTrue(dao.getAllGuideNotes().first().isEmpty())
        }

    @Test
    fun aBackupCarriesTheNoteToAFreshDatabaseAndTheBackupPasswordIsNotTheDeviceKey() =
        runTest {
            repository.save("RB-TD-001", SECRET, "r1")
            val archive = PayslipBackupService(database.payslipDao(), Dispatchers.Unconfined).export(BACKUP_PASSWORD).getOrThrow()
            assertTrue(CryptoHelper.decrypt(archive, CryptoHelper.getDatabaseSecretKey()).isFailure, "the archive is under the password")

            val fresh = newDatabase()
            try {
                PayslipBackupService(fresh.payslipDao(), Dispatchers.Unconfined).restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

                val restored = RoomGuideNotesRepository(fresh.payslipDao(), dispatcher = Dispatchers.Unconfined).observe().first()
                assertEquals(SECRET, restored.notes.single().text)
            } finally {
                fresh.close()
            }
        }
}
