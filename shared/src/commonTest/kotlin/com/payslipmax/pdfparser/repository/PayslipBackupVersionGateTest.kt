package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.backup.PayslipBackupRow
import com.payslipmax.pdfparser.repository.backup.PdfBackupRow
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A file written by a newer app may hold data this build cannot read. Restoring it would silently drop
 * that data, and a REPLACE would then make the device a copy of the partial result, so it is refused.
 */
class PayslipBackupVersionGateTest {
    private val dao = FakePayslipDao()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined)

    private fun backupWithVersion(version: Int): ByteArray {
        val json =
            buildJsonObject {
                put("version", version)
                put("encryptedPayslips", Json.encodeToJsonElement(ListSerializer(PayslipBackupRow.serializer()), listOf(goodRow("08/2024"))))
                put("pdfs", Json.encodeToJsonElement(ListSerializer(PdfBackupRow.serializer()), emptyList()))
                put("settings", JsonNull)
            }
        return CryptoHelper.encrypt(json.toString().encodeToByteArray(), Secret.of(BACKUP_PASSWORD)).getOrThrow()
    }

    @Test
    fun aBackupFromANewerAppIsRefusedWithAnUpdateMessageAndChangesNothing() =
        runTest {
            dao.seedDeviceMonth("01/2023")

            val result = service.restore(backupWithVersion(PortableBackup.CURRENT_VERSION + 1), Secret.of(BACKUP_PASSWORD), RestoreMode.REPLACE)

            val error = result.exceptionOrNull()
            assertTrue(error is UnsupportedBackupVersionException, "expected a version refusal but was $error")
            assertTrue(error.message!!.contains("update", ignoreCase = true), "the message must tell the user to update the app")
            assertEquals(listOf("01/2023"), dao.getAllPayslips().first().map { it.dateStr })
        }

    @Test
    fun theCurrentVersionIsStillAccepted() =
        runTest {
            service.restore(backupWithVersion(PortableBackup.CURRENT_VERSION), Secret.of(BACKUP_PASSWORD)).getOrThrow()

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
        }
}
