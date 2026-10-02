package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.PayslipPdfEntity
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
                put("encryptedPayslips", Json.encodeToJsonElement(ListSerializer(EncryptedPayslipEntity.serializer()), listOf(goodEntity("08/2024"))))
                put("pdfs", Json.encodeToJsonElement(ListSerializer(PayslipPdfEntity.serializer()), emptyList()))
                put("settings", JsonNull)
            }
        return CryptoHelper.encrypt(json.toString().encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()
    }

    @Test
    fun aBackupFromANewerAppIsRefusedWithAnUpdateMessageAndChangesNothing() =
        runTest {
            dao.seedDeviceMonth("01/2023")

            val result = service.restore(backupWithVersion(PortableBackup.CURRENT_VERSION + 1), BACKUP_PASSWORD, RestoreMode.REPLACE)

            val error = result.exceptionOrNull()
            assertTrue(error is UnsupportedBackupVersionException, "expected a version refusal but was $error")
            assertTrue(error.message!!.contains("update", ignoreCase = true), "the message must tell the user to update the app")
            assertEquals(listOf("01/2023"), dao.getAllPayslips().first().map { it.dateStr })
        }

    @Test
    fun theCurrentVersionIsStillAccepted() =
        runTest {
            service.restore(backupWithVersion(PortableBackup.CURRENT_VERSION), BACKUP_PASSWORD).getOrThrow()

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
        }
}
