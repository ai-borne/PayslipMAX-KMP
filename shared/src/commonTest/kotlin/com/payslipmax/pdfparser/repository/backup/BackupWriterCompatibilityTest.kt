package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.hexToByteArray
import com.payslipmax.pdfparser.database.toCorrectionEntity
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.createMockPayslip
import com.payslipmax.pdfparser.repository.seedDeviceMonth
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (owner decision, P2): the tolerant reader is for READING only. Shipped apps (1.5.0 and older) read a payslip payload with a strict
 * decoder, which fails on a missing field even when it is nullable. So every writer stays as it was: a null is written as `null` and
 * never left out. The strict `Json` here stands in for those shipped readers; a writer switched to drop nulls or defaults fails here.
 */
class BackupWriterCompatibilityTest {
    private val strict = Json

    private fun plain(hex: String): String = CryptoHelper.decrypt(hex.hexToByteArray(), BACKUP_PASSWORD).getOrThrow().decodeToString()

    @Test
    fun aPayslipPayloadKeepsItsNullsSoAStrictShippedReaderStillDecodesIt() {
        val payslip = createMockPayslip("08/2024").copy(taxAndSavings = null)

        val written = plain(payslip.toEncryptedEntity(BACKUP_PASSWORD).ciphertext)

        assertEquals(JsonNull, strict.parseToJsonElement(written).jsonObject["taxAndSavings"], "a null is written, not left out")
        assertEquals(payslip, strict.decodeFromString(ParsedPayslip.serializer(), written))
    }

    @Test
    fun correctionsKeepTheirNullOriginalsSoAStrictShippedReaderStillDecodesThem() {
        val corrections = listOf(SingleCorrection("basicPay", "BPAY", 61000.0, EntryCategory.EARNING, CorrectionType.ADDED, null, null, 5L))

        val written = plain(corrections.toCorrectionEntity("08/2024", BACKUP_PASSWORD).ciphertext)

        val row = strict.parseToJsonElement(written).jsonArray.single().jsonObject
        assertEquals(JsonNull, row["originalAmount"], "a null is written, not left out")
        assertEquals(JsonNull, row["originalCodeHead"], "a null is written, not left out")
        assertEquals(corrections, strict.decodeFromString(ListSerializer(SingleCorrection.serializer()), written))
    }

    @Test
    fun anExportedDocumentDecodesWithAStrictReader() =
        runTest {
            val dao = FakePayslipDao()
            dao.seedDeviceMonth("08/2024")

            val document = exportedDocument(dao, BACKUP_PASSWORD)

            assertEquals(PortableBackup.CURRENT_VERSION, strict.decodeFromJsonElement(PortableBackup.serializer(), document).version)
        }
}
