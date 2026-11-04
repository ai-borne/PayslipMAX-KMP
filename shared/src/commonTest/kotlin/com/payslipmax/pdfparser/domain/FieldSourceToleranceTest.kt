package com.payslipmax.pdfparser.domain

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.database.toHex
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.repository.backup.PayslipBackupRow
import com.payslipmax.pdfparser.repository.backupBytes
import com.payslipmax.pdfparser.repository.createMockPayslip
import com.payslipmax.pdfparser.repository.goodRow
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P5, D16; P10 "From P2" item 1): a later app may add a [FieldSource] value (say `OCR`). Before P5 one such value made
 * that whole payslip unreadable here, on restore and on the device after a rollback. The value is provenance only, so it is dropped from
 * `fieldSource` and the payslip itself is kept. What is not a value at all (a number, an object) is still a damaged payslip.
 */
class FieldSourceToleranceTest {
    private fun payloadWith(fieldSource: JsonElement): String {
        val payslip = Json.encodeToJsonElement(ParsedPayslip.serializer(), createMockPayslip("08/2024")).jsonObject
        return JsonObject(payslip + ("fieldSource" to fieldSource)).toString()
    }

    private val laterSources = JsonObject(mapOf("basicPay" to JsonPrimitive("GEOMETRY"), "incomeTax" to JsonPrimitive("OCR")))

    private fun sealedRow(
        payload: String,
        key: String,
    ) = PayslipBackupRow("08/2024", 2024, 8, "August", CryptoHelper.encrypt(payload.encodeToByteArray(), Secret.of(key)).getOrThrow().toHex())

    private suspend fun restore(vararg rows: PayslipBackupRow): Pair<FakePayslipDao, SkippedRows> {
        val dao = FakePayslipDao()
        val report = PayslipBackupService(dao, Dispatchers.Unconfined).restore(backupBytes(PortableBackup(2, rows.toList(), emptyList(), null)), Secret.of(BACKUP_PASSWORD), RestoreMode.REPLACE)
        assertTrue(report.isSuccess, "${report.exceptionOrNull()?.let { it::class.simpleName }}")
        return dao to report.getOrThrow().skipped
    }

    @Test
    fun aPayslipWithAValueALaterAppAddedRestoresWithoutThatValue() =
        runTest {
            val (dao, skipped) = restore(sealedRow(payloadWith(laterSources), BACKUP_PASSWORD))

            assertEquals(SkippedRows.NONE, skipped)
            assertEquals(mapOf("basicPay" to FieldSource.GEOMETRY), dao.getAllPayslips().first().single().toDomain(DEVICE_KEY_OF_FAKE).fieldSource)
        }

    @Test
    fun theDeviceReadsSuchAPayslipToo() {
        val row = sealedRow(payloadWith(laterSources), DEVICE_KEY)
        val entity = EncryptedPayslipEntity(row.dateStr, row.year, row.monthNum, row.monthName, row.ciphertext)

        assertEquals(mapOf("basicPay" to FieldSource.GEOMETRY), runCatching { entity.toDomain(DEVICE_KEY) }.getOrNull()?.fieldSource)
    }

    @Test
    fun aValueThatIsNotTextIsADamagedPayslipSkippedAndCountedNotACrash() =
        runTest {
            for (notText in listOf(JsonObject(mapOf("basicPay" to JsonPrimitive(7))), JsonObject(mapOf("basicPay" to JsonObject(emptyMap()))))) {
                val (dao, skipped) = restore(sealedRow(payloadWith(notText), BACKUP_PASSWORD), goodRow("09/2024"))

                assertEquals(SkippedRows(payslips = 1), skipped, "$notText")
                assertEquals(listOf("09/2024"), dao.getAllPayslips().first().map { it.dateStr }, "$notText")
            }
        }

    private companion object {
        const val DEVICE_KEY = "device-key-for-the-test"

        // The service re-seals restored payslips with the device's database key.
        val DEVICE_KEY_OF_FAKE: String get() = CryptoHelper.getDatabaseSecretKey()
    }
}
