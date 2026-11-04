package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.MemoryBackupSink
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.repository.goodRow
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (F2): Part II rows and the page archive's trailer are read by the same tolerant reader as the rest of a backup, so a later version
 * that writes null where this one has a default, or adds a key to the trailer, does not cost the officer an entry or every page.
 */
class Part2PayloadToleranceTest {
    private val key = "device-key-for-tests"
    private val base = """"v":1,"id":"entry-1","code":"SYNCODE1","from":{"y":2026,"m":3,"d":1},"createdAt":1,"updatedAt":2"""

    private fun sealedJson(
        json: String,
        password: String,
    ) = CryptoHelper.encrypt(json.encodeToByteArray(), Secret.of(password)).getOrThrow()

    @Test
    fun anEntryWithNullWhereADefaultExistsAndAKeyFromALaterVersionReadsWithItsDefaults() {
        val row = Part2EntryEntity("entry-1", sealedJson("""{$base,"data":null,"note":null,"futureField":{"at":9}}""", key))

        val entry = row.toEntry(key)

        assertEquals(listOf("", "", "", ""), entry.data)
        assertEquals("", entry.note)
    }

    @Test
    fun anOrderWithAKeyFromALaterVersionReads() {
        val json = """{"v":1,"id":"order-1","createdAt":1,"updatedAt":2,"futureField":true}"""

        assertEquals("order-1", Part2OrderEntity("order-1", sealedJson(json, key)).toOrder(key).id)
    }

    @Test
    fun aPageArchiveWhoseTrailerCarriesAKeyFromALaterVersionRestoresEveryPage() =
        runTest {
            val backup = PortableBackup(PortableBackup.CURRENT_VERSION, listOf(goodRow("08/2024")), emptyList(), null, part2Orders = listOf(orderRow(SyntheticBank.order())))
            val sink = MemoryBackupSink()
            val writer = PageArchiveWriter(sink)
            writer.begin(sealedJson(Json.encodeToString(PortableBackup.serializer(), backup), BACKUP_PASSWORD))
            val page = pageRow("page-1", "order-1", 0, SyntheticBank.jpeg(64))
            writer.add(page.id, page.orderId, page.position, page.sealed)
            val trailer = sealedJson("""{"pages":1,"checksum":"from a later version"}""", BACKUP_PASSWORD)
            sink.write(trailer)
            sink.write(ByteArray(4) { (trailer.size ushr (24 - 8 * it)).toByte() })
            val dao = FakePayslipDao()

            val report = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2)).restore(sink.toByteArray(), Secret.of(BACKUP_PASSWORD)).getOrThrow()

            assertEquals(SkippedRows.NONE, report.skipped)
            assertEquals(listOf("page-1"), dao.part2.allPageRefs().map { it.id })
        }
}
