package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.ArchiveKey
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.crypto.PasswordKey
import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.space.FreeSpaceGate
import com.payslipmax.pdfparser.repository.backup.newArchive
import com.payslipmax.pdfparser.repository.backup.openArchive
import com.payslipmax.pdfparser.testing.FakeBackupFiles
import com.payslipmax.pdfparser.testing.FakeFreeSpaceReader
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, section 4): the password must not outlive its use. One rule holds at every call site: whoever is handed a
 * [Secret] wipes it, on success, on failure and when cancelled. Each test makes the secret from a `CharArray` it keeps, so "wiped" means
 * those bytes are zero. A site that forgets leaves the password in memory until a garbage collection nobody can predict. Runs on the JVM
 * and on the iOS simulator.
 */
class BackupSecretZeroingTest {
    private val dao = FakePayslipDao()
    private val files = FakeBackupFiles()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2))

    private class Typed(
        text: String,
    ) {
        val chars = text.toCharArray()
        val secret get() = Secret.adopt(chars)
        val isWiped get() = chars.all { it == '\u0000' }
    }

    private val v6 get() = GoldenBackupCatalog.goldens.first { it.format == PortableBackup.CURRENT_VERSION }
    private val legacy get() = GoldenBackupCatalog.goldens.first { it.format == 3 }

    private suspend fun aV6Backup(password: String = BACKUP_PASSWORD): ByteArray {
        dao.seedDeviceMonth("08/2024")
        return service.export(Secret.of(password)).getOrThrow()
    }

    // ---- Back Up -------------------------------------------------------------------------------------------------------------

    @Test
    fun backUpWipesThePasswordAfterABackupThatReadsBack() =
        runTest {
            dao.seedDeviceMonth("08/2024")
            val typed = Typed(BACKUP_PASSWORD)

            service.backUp(files, typed.secret).getOrThrow()

            assertTrue(typed.isWiped)
            assertEquals(1, files.verifyCount, "the read-back needed the password a second time, so it must have had its own copy")
        }

    @Test
    fun backUpWipesThePasswordWhenAFailureStopsItBeforeAnythingIsWritten() =
        runTest {
            dao.seedDeviceMonth("08/2024")
            val noRoom = PayslipBackupService(dao, Dispatchers.Unconfined, space = FreeSpaceGate(FakeFreeSpaceReader().apply { free = 0 }), part2Backup = Part2Backup(dao.part2))
            val typed = Typed(BACKUP_PASSWORD)

            val result = noRoom.backUp(files, typed.secret)

            assertTrue(result.exceptionOrNull() is BackupStorageFullException)
            assertTrue(typed.isWiped, "a refused backup is still a use of the password")
        }

    @Test
    fun backUpWipesThePasswordWhenTheOperationIsCancelledBeforeItRuns() =
        runTest {
            dao.seedDeviceMonth("08/2024")
            val typed = Typed(BACKUP_PASSWORD)

            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    cancel()
                    service.backUp(files, typed.secret)
                }
            job.join()

            assertTrue(job.isCancelled)
            assertTrue(typed.isWiped, "a cancelled backup must not leave the password behind")
        }

    @Test
    fun exportAndExportToWipeThePassword() =
        runTest {
            dao.seedDeviceMonth("08/2024")
            val viaExport = Typed(BACKUP_PASSWORD)
            val viaExportTo = Typed(BACKUP_PASSWORD)

            service.export(viaExport.secret).getOrThrow()
            service.exportTo(MemoryBackupSink(), viaExportTo.secret).getOrThrow()

            assertTrue(viaExport.isWiped, "export")
            assertTrue(viaExportTo.isWiped, "exportTo")
        }

    @Test
    fun exportToWipesThePasswordWhenCancelled() =
        runTest {
            val typed = Typed(BACKUP_PASSWORD)

            launch(start = CoroutineStart.UNDISPATCHED) {
                cancel()
                service.exportTo(MemoryBackupSink(), typed.secret)
            }.join()

            assertTrue(typed.isWiped)
        }

    // ---- Restore -------------------------------------------------------------------------------------------------------------

    @Test
    fun prepareOfAV6FileWipesThePasswordAsSoonAsTheKeyIsDerivedWhileThePreviewStillWorks() =
        runTest {
            val bytes = aV6Backup()
            val typed = Typed(BACKUP_PASSWORD)

            val prepared = service.prepare(MemoryBackupSource(bytes), typed.secret).getOrThrow()

            assertTrue(typed.isWiped, "the archive key replaces the password for a format 6 file")
            assertEquals(1, prepared.preview.payslips)
            service.commit(prepared, RestoreMode.MERGE).getOrThrow()
            prepared.close()
        }

    @Test
    fun prepareOfAnOlderFileKeepsThePasswordOnlyWhileThePreviewIsOnScreen() =
        runTest {
            val typed = Typed(legacy.password)

            val prepared = service.prepare(MemoryBackupSource(GoldenBackupCatalog.bytes(legacy.file)), typed.secret).getOrThrow()

            assertFalse(typed.isWiped, "formats 1 to 5 seal every row with the password, so a MERGE after the preview still needs it")
            service.commit(prepared, RestoreMode.MERGE).getOrThrow()
            assertFalse(typed.isWiped, "a commit does not end the preview")

            prepared.close()

            assertTrue(typed.isWiped, "closing the preview is what ends the password's life")
        }

    @Test
    fun prepareWipesThePasswordWhenItFails() =
        runTest {
            val bytes = aV6Backup()
            val wrongV6 = Typed("not-the-password")
            val wrongLegacy = Typed("not-the-password")
            val notABackup = Typed(BACKUP_PASSWORD)
            val newer = Typed(BACKUP_PASSWORD)

            val results =
                listOf(
                    service.prepare(MemoryBackupSource(bytes), wrongV6.secret),
                    service.prepare(MemoryBackupSource(GoldenBackupCatalog.bytes(legacy.file)), wrongLegacy.secret),
                    service.prepare(MemoryBackupSource(ByteArray(10)), notABackup.secret),
                    service.prepare(MemoryBackupSource(bytes.copyOf().also { it[ENVELOPE_VERSION_AT] = 99 }), newer.secret),
                )

            assertTrue(results.all { it.isFailure })
            assertTrue(wrongV6.isWiped, "a wrong password on a format 6 file")
            assertTrue(wrongLegacy.isWiped, "a wrong password on an older file (a failed decrypt)")
            assertTrue(notABackup.isWiped, "a file that is not a backup")
            assertTrue(newer.isWiped, "a file from a later app")
        }

    @Test
    fun prepareWipesThePasswordWhenCancelledBeforeItRuns() =
        runTest {
            val bytes = aV6Backup()
            val typed = Typed(BACKUP_PASSWORD)

            launch(start = CoroutineStart.UNDISPATCHED) {
                cancel()
                service.prepare(MemoryBackupSource(bytes), typed.secret)
            }.join()

            assertTrue(typed.isWiped)
        }

    @Test
    fun prepareWipesThePasswordWhenThereIsNoRoomToRestore() =
        runTest {
            val bytes = aV6Backup()
            val noRoom = PayslipBackupService(FakePayslipDao(), Dispatchers.Unconfined, space = FreeSpaceGate(FakeFreeSpaceReader().apply { free = 0 }))
            val typed = Typed(BACKUP_PASSWORD)

            assertTrue(noRoom.prepare(MemoryBackupSource(bytes), typed.secret).exceptionOrNull() is BackupStorageFullException)

            assertTrue(typed.isWiped)
        }

    @Test
    fun restoreWipesThePasswordWhateverTheOutcome() =
        runTest {
            val bytes = aV6Backup()
            val good = Typed(BACKUP_PASSWORD)
            val wrong = Typed("nope")
            val olderGood = Typed(legacy.password)

            service.restore(bytes, good.secret).getOrThrow()
            assertTrue(service.restore(bytes, wrong.secret).isFailure)
            service.restore(GoldenBackupCatalog.bytes(legacy.file), olderGood.secret).getOrThrow()

            assertTrue(good.isWiped, "a restore that worked")
            assertTrue(wrong.isWiped, "a restore with the wrong password")
            assertTrue(olderGood.isWiped, "a restore of an older file, whose preview closed with the restore")
        }

    // ---- The layers underneath -----------------------------------------------------------------------------------------------

    @Test
    fun openArchiveWipesThePasswordForAV6FileAndWhenItFailsAndHandsItOnForAnOlderOne() =
        runTest {
            val bytes = aV6Backup()
            val v6 = Typed(BACKUP_PASSWORD)
            val wrong = Typed("nope")
            val older = Typed(legacy.password)

            openArchive(MemoryBackupSource(bytes), v6.secret).close()
            assertFailsWith<DecryptionFailedException> { openArchive(MemoryBackupSource(bytes), wrong.secret) }
            val opened = openArchive(MemoryBackupSource(GoldenBackupCatalog.bytes(legacy.file)), older.secret)

            assertTrue(v6.isWiped, "format 6: the key replaces the password")
            assertTrue(wrong.isWiped, "a failed open")
            assertFalse(older.isWiped, "an older file's key owns the password until it is closed")
            opened.close()
            assertTrue(older.isWiped)
        }

    @Test
    fun newArchiveAndDeriveWipeThePassword() {
        val forArchive = Typed(BACKUP_PASSWORD)
        val forDerive = Typed(BACKUP_PASSWORD)

        newArchive(forArchive.secret).close()
        ArchiveKey.derive(forDerive.secret, ByteArray(16), 10_000, ByteArray(4)).close()

        assertTrue(forArchive.isWiped, "newArchive")
        assertTrue(forDerive.isWiped, "ArchiveKey.derive")
    }

    @Test
    fun theKeyDerivationAndCipherOnlyBorrowThePasswordBecauseTheirCallerStillOwnsIt() {
        val typed = Typed(BACKUP_PASSWORD)
        val secret = typed.secret

        val sealed = CryptoHelper.encrypt("row".encodeToByteArray(), secret).getOrThrow()
        val opened = CryptoHelper.decrypt(sealed, secret).getOrThrow()
        val key = CryptoHelper.pbkdf2(secret, ByteArray(16), 10)

        assertContentEquals("row".encodeToByteArray(), opened)
        assertEquals(32, key.size)
        assertFalse(typed.isWiped, "a legacy key seals thousands of rows with one password; each call must not use it up")
        secret.close()
        assertTrue(typed.isWiped)
    }

    @Test
    fun aPasswordKeyWipesItsPasswordWhenClosed() {
        val typed = Typed(BACKUP_PASSWORD)
        val key = PasswordKey(typed.secret)
        key.seal("x".encodeToByteArray()).getOrThrow()
        assertFalse(typed.isWiped)

        key.close()

        assertTrue(typed.isWiped)
    }
}

/** The envelope version byte follows the 4-byte "PCDX" mark. */
private const val ENVELOPE_VERSION_AT = 4
