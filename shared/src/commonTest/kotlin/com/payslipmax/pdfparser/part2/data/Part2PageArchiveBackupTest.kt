package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: scanned pages travel as binary records after the archive's document (a page archive), not inside its JSON, so a full vault is
 * not copied several times on the way out. These pin how that archive is chosen (no pages: the old single blob), that it round trips
 * on REPLACE and MERGE, and that a damaged one fails the whole restore loudly while the device keeps everything.
 */
class Part2PageArchiveBackupTest {
    private lateinit var source: FakePayslipDao
    private lateinit var target: FakePayslipDao
    private lateinit var sourceService: PayslipBackupService
    private lateinit var targetService: PayslipBackupService

    @BeforeTest
    fun setUp() {
        source = FakePayslipDao()
        target = FakePayslipDao()
        sourceService = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2))
        targetService = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2))
    }

    private suspend fun targetDrafts() = target.part2.allDrafts().associate { it.id to it.toDraft(deviceKey) }

    private fun draft(
        id: String,
        updatedAt: Long,
    ) = SyntheticPart2Drafts.fullDraft(id, updatedAt)

    @Test
    fun anArchiveWithoutPagesIsTheOldSingleBlobAndOneWithPagesIsAPageArchiveAsLargeAsTheVault() =
        runTest {
            source.putDraft(draft("d1", 1))
            val withoutPages = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            assertTrue(PageArchive.metaOf(withoutPages) == null, "no pages: the same one encrypted document as v1-v4")

            source.putOrder(SyntheticPart2Drafts.order("o1"))
            repeat(4) { source.putPage("p$it", "o1", it, SyntheticPart2Drafts.jpeg(200_000, it)) }
            val withPages = sourceService.export(BACKUP_PASSWORD).getOrThrow()

            val vault = source.part2.vaultBytes()
            assertTrue(PageArchive.metaOf(withPages) != null)
            assertTrue(withPages.size < vault + 60_000, "the archive is the vault plus the document, not a Base64 JSON copy: ${withPages.size} vs $vault")
            assertContentEquals(withoutPages.size.let { decodeArchive(withoutPages).part2Drafts.map { it.id } }, decodeArchive(withPages).part2Drafts.map { it.id })
        }

    @Test
    fun aPageArchiveRestoresOnReplaceAndMergeAndANewDevicesPagesAreReadableByIt() =
        runTest {
            val jpeg = SyntheticPart2Drafts.jpeg(30_000, 2)
            source.putOrder(SyntheticPart2Drafts.order("o1"))
            source.putPage("p1", "o1", 0, jpeg)
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()

            RestoreMode.entries.forEach { mode ->
                target.clearAllUserData()
                targetService.restore(archive, BACKUP_PASSWORD, mode).getOrThrow()
                assertContentEquals(jpeg, target.part2.page("p1")!!.openPage(deviceKey), "page restored ($mode)")
            }
        }

    @Test
    fun aDamagedPageArchiveFailsTheWholeRestoreAndTheDeviceKeepsEverything() =
        runTest {
            source.putOrder(SyntheticPart2Drafts.order("o1"))
            repeat(3) { source.putPage("p$it", "o1", it, SyntheticPart2Drafts.jpeg(2000, it)) }
            val good = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.putDraft(draft("device", 1))
            val cut = good.copyOf(good.size - 40)
            val missingARecord = archive(orders = listOf(orderRow(SyntheticPart2Drafts.order("o1"))), pages = listOf(pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(100))), declared = 2)
            val extraRecord = archive(orders = listOf(orderRow(SyntheticPart2Drafts.order("o1"))), pages = listOf(pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(100)), pageRow("p2", "o1", 1, SyntheticPart2Drafts.jpeg(100))), declared = 1)

            assertTrue(targetService.restore(cut, BACKUP_PASSWORD, RestoreMode.REPLACE).isFailure, "a cut file fails like any damaged archive")
            for ((name, bytes) in listOf("missing record" to missingARecord, "extra record" to extraRecord)) {
                val error = targetService.restore(bytes, BACKUP_PASSWORD, RestoreMode.REPLACE).exceptionOrNull()

                assertTrue(error is BackupDamagedException, "$name must be refused as a damaged archive but was $error")
                assertTrue(error.cause == null && error.message!!.none { it.isDigit() }, "$name: a fixed message with no content")
            }
            assertEquals(setOf("device"), targetDrafts().keys)
        }

    @Test
    fun aWrongPasswordOnAPageArchiveIsRefusedLikeAnyOtherArchive() =
        runTest {
            source.putOrder(SyntheticPart2Drafts.order("o1"))
            source.putPage("p1", "o1", 0, SyntheticPart2Drafts.jpeg(500))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()

            assertTrue(targetService.restore(archive, "not the password", RestoreMode.REPLACE).isFailure)
        }

    @Test
    fun aRecordThatClaimsMoreBytesThanTheFileHoldsButWithinThePageLimitDamagesTheArchive() =
        runTest {
            val good = archive(orders = listOf(orderRow(SyntheticPart2Drafts.order("o1"))), pages = listOf(pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(300))))
            val lengthAt = 8 + PageArchive.metaOf(good)!!.size + (1 + "p1".length + 1 + "o1".length + 4) // the record's own length field
            val lying = good.copyOf().also { bytes -> listOf(0x00, 0x06, 0x1A, 0x80).forEachIndexed { i, b -> bytes[lengthAt + i] = b.toByte() } }
            target.putDraft(draft("device", 1))

            val error = targetService.restore(lying, BACKUP_PASSWORD, RestoreMode.REPLACE).exceptionOrNull()

            assertTrue(error is BackupDamagedException, "expected a damaged-archive refusal but was $error")
            assertEquals(setOf("device"), targetDrafts().keys)
        }

    @Test
    fun aTrailerThatCountsFewerThanZeroPagesIsADamagedArchive() =
        runTest {
            val forged = archive(orders = listOf(orderRow(SyntheticPart2Drafts.order("o1"))), pages = listOf(pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(100))), declared = -1)

            assertTrue(targetService.restore(forged, BACKUP_PASSWORD, RestoreMode.REPLACE).exceptionOrNull() is BackupDamagedException)
        }
}
