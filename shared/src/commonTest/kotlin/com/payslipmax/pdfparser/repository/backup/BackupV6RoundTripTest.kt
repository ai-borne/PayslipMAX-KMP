package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.crypto.ArchiveKey
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.hexToByteArray
import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.deviceKey
import com.payslipmax.pdfparser.part2.data.openPage
import com.payslipmax.pdfparser.part2.data.putEntry
import com.payslipmax.pdfparser.part2.data.putOrder
import com.payslipmax.pdfparser.part2.data.putPage
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.MemoryBackupSource
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.repository.seedDeviceMonth
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P3, owner decisions): a v6 archive keeps every row's shape (`ciphertext`, `data`) but seals rows and pages with the one
 * key its header derives, instead of running PBKDF2 again for every row and page, which made a large restore slow and is what the outer
 * key now protects. The device database is untouched: what a restore writes is still sealed per row with the device key. And the
 * officer's data must come back exactly, Part II pages included.
 */
class BackupV6RoundTripTest {
    private val source = FakePayslipDao()
    private val jpegs = listOf(SyntheticBank.jpeg(3000, 1), SyntheticBank.jpeg(4000, 2))

    private suspend fun seeded(): ByteArray {
        source.seedDeviceMonth("07/2024")
        source.seedDeviceMonth("08/2024")
        source.insertSettings(AppSettingsEntity(appTheme = "dark"))
        source.putOrder(SyntheticBank.order("o1"))
        source.putEntry(SyntheticBank.entry("e1", 10, orderId = "o1"))
        source.putEntry(SyntheticBank.entry("e2", 20))
        jpegs.forEachIndexed { i, jpeg -> source.putPage("p$i", "o1", i, jpeg) }
        return PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2)).export(BACKUP_PASSWORD).getOrThrow()
    }

    @Test
    fun everythingAV6BackupCarriesComesBackAndTheDeviceKeepsItsOwnRowEncryption() =
        runTest {
            val file = seeded()
            val target = FakePayslipDao()
            val pageKeys = mutableListOf<String?>()
            val part2 = Part2Backup(target.part2, openPage = { page, key -> page.openPage(key).also { pageKeys += key::class.simpleName } })

            val report = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = part2).restore(file, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(SkippedRows.NONE, report.skipped)
            val payslips = target.getAllPayslips().first().sortedBy { it.dateStr }
            assertEquals(source.getAllPayslips().first().sortedBy { it.dateStr }.map { it.toDomain() }, payslips.map { it.toDomain() })
            for (row in payslips) assertTrue(CryptoHelper.decrypt(row.ciphertext.hexToByteArray(), deviceKey).isSuccess, "device rows stay per-row")
            assertEquals(2, target.getAllCorrections().first().size)
            assertEquals(2, target.getAllGuideNotes().first().size)
            assertEquals("dark", target.getSettings()?.appTheme)
            assertEquals(setOf("e1", "e2"), target.part2.allEntries().map { it.id }.toSet())
            assertEquals(listOf("o1"), target.part2.allOrders().map { it.id })
            jpegs.forEachIndexed { i, jpeg -> assertContentEquals(jpeg, target.part2.page("p$i")!!.openPage(deviceKey)) }
            assertTrue(pageKeys.isNotEmpty() && pageKeys.all { it == "ArchiveKey" }, "pages open with the archive key: $pageKeys")
        }

    @Test
    fun everyRowIsSealedWithTheOneArchiveKeyAndNeverPerRowWithThePassword() =
        runTest {
            val opened = openArchive(MemoryBackupSource(seeded()), BACKUP_PASSWORD)
            val document = BackupJson.decode(PortableBackup.serializer(), opened.document.decodeToString())

            assertTrue(opened.key is ArchiveKey, "a v6 file opens with its archive key")
            assertEquals(PortableBackup.CURRENT_VERSION, document.version)
            val rows =
                document.encryptedPayslips.map { it.ciphertext.hexToByteArray() } +
                    document.corrections.map { it.ciphertext.hexToByteArray() } +
                    document.guideNotes.map { it.ciphertext.hexToByteArray() } +
                    (document.part2Entries + document.part2Orders).map { Base64.decode(it.data) }
            assertEquals(2 + 2 + 2 + 3, rows.size)
            for (row in rows) {
                assertEquals(row.size, opened.key.open(row).getOrThrow().size + ArchiveKey.OVERHEAD_BYTES, "no per-row salt")
                assertTrue(CryptoHelper.decrypt(row, BACKUP_PASSWORD).isFailure, "no row is sealed per row with the password")
            }
        }
}
