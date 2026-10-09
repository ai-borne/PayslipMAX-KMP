package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.GuideNoteEntity
import com.payslipmax.pdfparser.database.hexToByteArray
import com.payslipmax.pdfparser.database.toEntity
import com.payslipmax.pdfparser.database.toNote
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Backup v4 adds the user's private Guide notes. They travel only inside the user's own password-encrypted archive, under
 * the backup password (so an archive does not depend on one device's key), and come back under the restoring device's key.
 * A backup is the only copy if a phone is lost, so a damaged note must never cost the user the payslips around it.
 */
class PayslipBackupServiceV4Test {
    private lateinit var source: FakePayslipDao
    private lateinit var target: FakePayslipDao
    private lateinit var sourceService: PayslipBackupService
    private lateinit var targetService: PayslipBackupService

    private val deviceKey get() = CryptoHelper.getDatabaseSecretKey()

    @BeforeTest
    fun setUp() {
        source = FakePayslipDao()
        target = FakePayslipDao()
        sourceService = PayslipBackupService(source, Dispatchers.Unconfined)
        targetService = PayslipBackupService(target, Dispatchers.Unconfined)
    }

    private fun note(
        cardId: String,
        text: String = "note on $cardId",
        at: Long = 10L,
    ) = assertNotNull(GuideNote.of(cardId, text, "rev00001", at))

    private suspend fun decodeArchive(archive: ByteArray): PortableBackup {
        val json = CryptoHelper.decrypt(archive, BACKUP_PASSWORD).getOrThrow().decodeToString()
        return Json.decodeFromString(PortableBackup.serializer(), json)
    }

    private suspend fun storedNotes() = target.getAllGuideNotes().first().associate { it.cardId to it.toNote(deviceKey) }

    @Test
    fun theCurrentVersionIsFourAndExportsWriteIt() =
        runTest {
            assertEquals(4, PortableBackup.CURRENT_VERSION)

            assertEquals(4, decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow()).version)
        }

    @Test
    fun notesInTheArchiveAreEncryptedWithTheBackupPasswordNotTheDeviceKeyAndHoldNoPlaintext() =
        runTest {
            source.insertGuideNote(note("RB-TD-001", "my secret doubt about DA").toEntity(deviceKey))

            val backup = decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow())

            val stored = backup.guideNotes.single()
            assertEquals("my secret doubt about DA", stored.toNote(BACKUP_PASSWORD).text)
            assertTrue(CryptoHelper.decrypt(stored.ciphertext.hexToByteArray(), deviceKey).isFailure, "an archive must not depend on one device's key")
            assertFalse(stored.ciphertext.contains("secret"))
        }

    @Test
    fun notesSurviveAnExportAndAReplaceRestoreAndAreReEncryptedUnderTheRestoringDevicesKey() =
        runTest {
            source.insertGuideNote(note("RB-TD-001", at = 5L).toEntity(deviceKey))
            source.insertGuideNote(note("RB-TD-002", at = 6L).toEntity(deviceKey))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.insertGuideNote(note("RB-ONLY-HERE").toEntity(deviceKey))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(setOf("RB-TD-001", "RB-TD-002"), storedNotes().keys, "REPLACE leaves an exact copy of the backup")
            assertEquals(note("RB-TD-001", at = 5L), storedNotes().getValue("RB-TD-001"))
            val bytes = target.getAllGuideNotes().first().first().ciphertext.hexToByteArray()
            assertTrue(CryptoHelper.decrypt(bytes, BACKUP_PASSWORD).isFailure, "stored under the device key, not the backup password")
        }

    @Test
    fun mergeKeepsDeviceOnlyNotesAddsBackupOnlyNotesAndNewerUpdatedAtWinsOnTheSameCard() =
        runTest {
            source.insertGuideNote(note("RB-BACKUP-NEWER", "backup newer", at = 90L).toEntity(deviceKey))
            source.insertGuideNote(note("RB-DEVICE-NEWER", "backup older", at = 10L).toEntity(deviceKey))
            source.insertGuideNote(note("RB-TIE", "backup on tie", at = 50L).toEntity(deviceKey))
            source.insertGuideNote(note("RB-BACKUP-ONLY", at = 1L).toEntity(deviceKey))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.insertGuideNote(note("RB-BACKUP-NEWER", "device older", at = 20L).toEntity(deviceKey))
            target.insertGuideNote(note("RB-DEVICE-NEWER", "device newer", at = 80L).toEntity(deviceKey))
            target.insertGuideNote(note("RB-TIE", "device on tie", at = 50L).toEntity(deviceKey))
            target.insertGuideNote(note("RB-DEVICE-ONLY", at = 3L).toEntity(deviceKey))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            val texts = storedNotes().mapValues { it.value.text }
            assertEquals("backup newer", texts.getValue("RB-BACKUP-NEWER"))
            assertEquals("device newer", texts.getValue("RB-DEVICE-NEWER"), "a restore must never overwrite a newer edit made on this phone")
            assertEquals("backup on tie", texts.getValue("RB-TIE"))
            assertTrue("RB-DEVICE-ONLY" in texts && "RB-BACKUP-ONLY" in texts)
            assertEquals(5, texts.size)
        }

    @Test
    fun mergeLetsTheBackupWinWhenTheDevicesRowForThatCardCannotBeRead() =
        runTest {
            source.insertGuideNote(note("RB-TD-001", "from backup").toEntity(deviceKey))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.insertGuideNote(GuideNoteEntity("RB-TD-001", "deadbeef"))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            assertEquals("from backup", storedNotes().getValue("RB-TD-001").text)
        }

    @Test
    fun aDamagedNoteIsSkippedAndEverythingElseStillRestoresInBothModes() =
        runTest {
            val good = note("RB-GOOD", "good one").toEntity(BACKUP_PASSWORD)
            val garbage = GuideNoteEntity("RB-BAD-1", "deadbeef")
            val wrongKey = note("RB-BAD-2").toEntity("some other password")
            val movedRow = GuideNoteEntity("RB-BAD-3", good.ciphertext)
            val backup =
                backupBytes(
                    PortableBackup(
                        version = 4,
                        encryptedPayslips = listOf(goodEntity("08/2024")),
                        pdfs = emptyList(),
                        settings = null,
                        guideNotes = listOf(garbage, good, wrongKey, movedRow),
                    ),
                )

            RestoreMode.entries.forEach { mode ->
                target.seedDeviceMonth("01/2023")

                targetService.restore(backup, BACKUP_PASSWORD, mode).getOrThrow()

                assertTrue(target.getPayslipByDate("08/2024") != null, "the payslips must not be lost to a bad note ($mode)")
                val keys = storedNotes().keys
                assertTrue("RB-GOOD" in keys, "the readable note is restored ($mode)")
                assertTrue(keys.none { it.startsWith("RB-BAD") }, "no damaged, wrong-key or moved note is restored ($mode)")
            }
        }

    @Test
    fun aDeviceNoteThatCannotBeReadIsLeftOutOfAnExportNotFatalToIt() =
        runTest {
            source.insertGuideNote(note("RB-TD-001").toEntity(deviceKey))
            source.insertGuideNote(GuideNoteEntity("RB-TD-002", "deadbeef"))
            source.insertGuideNote(note("RB-TD-003").toEntity("a key this device no longer has"))

            val backup = decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow())

            assertEquals(listOf("RB-TD-001"), backup.guideNotes.map { it.cardId })
        }

    @Test
    fun aVersion3BackupStillRestoresAndNeedsNoNotesField() =
        runTest {
            val v3 = backupBytes(PortableBackup(3, listOf(goodEntity("08/2024")), emptyList(), null))
            val json = CryptoHelper.decrypt(v3, BACKUP_PASSWORD).getOrThrow().decodeToString()
            assertFalse(json.contains("\"guideNotes\":[{"), "no notes in this archive")

            targetService.restore(v3, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(listOf("08/2024"), target.getAllPayslips().first().map { it.dateStr })
            assertTrue(target.getAllGuideNotes().first().isEmpty())
        }

    @Test
    fun aVersion3BackupInMergeModeLeavesTheDevicesNotesAlone() =
        runTest {
            target.insertGuideNote(note("RB-TD-001").toEntity(deviceKey))
            val v3 = backupBytes(PortableBackup(3, listOf(goodEntity("08/2024")), emptyList(), null))

            targetService.restore(v3, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            assertEquals(setOf("RB-TD-001"), storedNotes().keys)
        }

    @Test
    fun aReplaceRestoreOfAnOlderBackupLeavesNoNotesBecauseTheDeviceBecomesACopyOfTheBackup() =
        runTest {
            // Same rule as corrections and letters: REPLACE means "exactly this archive". Pinned so a change is deliberate.
            target.insertGuideNote(note("RB-TD-001").toEntity(deviceKey))
            val v3 = backupBytes(PortableBackup(3, listOf(goodEntity("08/2024")), emptyList(), null))

            targetService.restore(v3, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertTrue(target.getAllGuideNotes().first().isEmpty())
        }

    @Test
    fun deleteAllDataAlsoErasesTheNotes() =
        runTest {
            target.insertGuideNote(note("RB-TD-001").toEntity(deviceKey))

            target.clearAllUserData()

            assertTrue(target.getAllGuideNotes().first().isEmpty())
        }
}
