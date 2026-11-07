package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.PasswordKey
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.MemoryBackupSource
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * WHY: scanned orders are the officer's photo library and a backup's pages are untrusted bytes up to hundreds of MB. [restorePages]
 * must judge every record's header against the limits BEFORE reading its page, keep an archive that lies about its own length from
 * writing anything, and leave pages the device already holds alone. Tested on the function itself, with the exact ids that survive.
 */
class Part2PageRestoreTest {
    private val device = FakePayslipDao()
    private val orders = setOf("o1")

    private suspend fun restore(
        pages: List<RawPage>,
        orderIds: Set<String> = orders,
        merge: Boolean = false,
        vaultMax: Long = Part2Limits.VAULT_BYTES_MAX,
        declared: Int = pages.size,
    ): PageRestore =
        restorePages(device.part2, MemoryBackupSource(archive(pages = pages, declared = declared)), orderIds, PasswordKey(BACKUP_PASSWORD), deviceKey, merge, vaultMax) { page, key ->
            page.openPage(key)
        }

    private fun page(
        id: String,
        position: Int = 0,
        orderId: String = "o1",
        seed: Int = 1,
    ) = pageRow(id, orderId, position, SyntheticBank.jpeg(400, seed))

    @Test
    fun aValidPageIsReSealedForThisDeviceAndItsBytesSurvive() =
        runTest {
            val jpeg = SyntheticBank.jpeg(400, 7)

            val result = restore(listOf(pageRow("p1", "o1", 0, jpeg)))
            val written = result.pages.toList().single()

            assertEquals(1, result.count)
            assertEquals(0, result.skipped)
            assertEquals(Triple("p1", "o1", 0), Triple(written.id, written.orderId, written.position))
            assertContentEquals(jpeg, written.openPage(deviceKey), "the page opens with the device key and is byte for byte the original")
        }

    @Test
    fun anArchiveWithNoPagesRestoresNothing() =
        runTest {
            val result = restorePages(device.part2, MemoryBackupSource(archive()), orders, PasswordKey(BACKUP_PASSWORD), deviceKey, false, Part2Limits.VAULT_BYTES_MAX) { p, k -> p.openPage(k) }

            assertEquals(0, result.count)
            assertEquals(0, result.skipped)
        }

    @Test
    fun aPageForAnOrderTheRestoreDoesNotKeepIsSkipped() =
        runTest {
            val result = restore(listOf(page("p1"), page("p2", orderId = "ghost")))

            assertEquals(listOf("p1"), result.pages.map { it.id }.toList())
            assertEquals(1, result.skipped)
        }

    @Test
    fun anOrderKeepsAtMostTenPagesAndTheRestAreCounted() =
        runTest {
            val pages = (0..Part2Limits.PAGES_PER_ORDER_MAX).map { page("p$it", position = it) }

            val result = restore(pages)

            assertEquals(Part2Limits.PAGES_PER_ORDER_MAX, result.count)
            assertEquals(1, result.skipped)
        }

    @Test
    fun theVaultCeilingStopsPagesThatWouldPassIt() =
        runTest {
            val size = page("p1").sealed.size.toLong()

            val result = restore(listOf(page("p1"), page("p2", position = 1)), vaultMax = size + size / 2)

            assertEquals(listOf("p1"), result.pages.map { it.id }.toList())
            assertEquals(1, result.skipped)
        }

    @Test
    fun aDuplicatePageIdIsKeptOnceAndTheCopyIsCounted() =
        runTest {
            val result = restore(listOf(page("p1"), page("p1", position = 1, seed = 2)))

            assertEquals(1, result.count)
            assertEquals(1, result.skipped)
        }

    @Test
    fun aPageSealedForAnotherPasswordIsSkippedNotFatal() =
        runTest {
            val foreign = pageRow("p2", "o1", 1, SyntheticBank.jpeg(400, 3), password = "some other password")

            val result = restore(listOf(page("p1"), foreign))

            assertEquals(listOf("p1"), result.pages.map { it.id }.toList())
            assertEquals(1, result.skipped)
        }

    @Test
    fun onMergeAPageTheDeviceAlreadyHoldsIsLeftAloneAndNotCountedAsSkipped() =
        runTest {
            device.putPage("p1", "o1", 0, SyntheticBank.jpeg(400, 9))

            val result = restore(listOf(page("p1"), page("p2", position = 1)), merge = true)

            assertEquals(listOf("p2"), result.pages.map { it.id }.toList())
            assertEquals(0, result.skipped)
        }

    @Test
    fun aTrailerThatCountsMorePagesThanTheFileHoldsFailsTheWholeRestore() =
        runTest {
            assertFailsWith<BackupDamagedException> { restore(listOf(page("p1")), declared = 2) }
        }

    @Test
    fun aTrailerThatCountsFewerPagesThanTheFileHoldsFailsTheWholeRestore() =
        runTest {
            assertFailsWith<BackupDamagedException> { restore(listOf(page("p1"), page("p2", position = 1)), declared = 1) }
        }
}
