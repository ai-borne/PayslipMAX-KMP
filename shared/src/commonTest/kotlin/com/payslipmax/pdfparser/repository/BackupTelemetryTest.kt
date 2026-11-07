package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.DecryptionFailedException
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.telemetry.TelemetrySanitizer
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P6, F5, section 4): a failed restore used to leave nothing to diagnose, and a crash report is the one thing that
 * leaves the device. So each failure sends exactly one non-fatal with a fixed message and three keys of fixed codes (what failed, the
 * format, which operation), never the exception or its text, and nothing at all when the officer turned telemetry off.
 */
class BackupTelemetryTest {
    private val reporter = FakeCrashReporter()
    private val settings = FakePayslipDao()
    private val telemetry = BackupTelemetry(reporter, settings)

    /** Messages a real failure can carry: a path, a PAN-like id, the crypto library's words, kotlinx quoting a payslip. */
    private val leaky =
        listOf(
            "/data/user/0/in.aiborne.payslipmax/cache/PayslipMax-backup.pcda",
            "Row for ABCDE1234F, net pay 98765.43",
            "error:1e000065:Cipher functions:OPENSSL_internal:BAD_DECRYPT",
            "Unexpected JSON token at offset 40: {\"dateStr\":\"03/2024\",\"basicPay\":98765.43}",
        )

    private val leakyFailures: List<Throwable> =
        leaky.flatMap { text ->
            listOf(IllegalStateException(text), RuntimeException(text, BackupDamagedException()), SerializationException(text))
        }

    @Test
    fun aFailureSendsOneNonFatalWithAFixedMessageAndCodeOnlyKeys() =
        runTest {
            telemetry.report(BackupOp.RESTORE, DecryptionFailedException())

            val (event, keys) = reporter.exceptions.single()
            assertIs<BackupFailureEvent>(event)
            assertEquals("backup_failed", event.message)
            assertNull(event.cause, "the real exception never rides along")
            assertEquals(
                mapOf("backup_failure_code" to "wrong_password", "backup_format_version" to "unknown", "backup_op" to "restore"),
                keys,
            )
            assertTrue(reporter.logs.isEmpty() && reporter.keys.isEmpty(), "nothing else is sent")
        }

    @Test
    fun noExceptionTextReachesTheReportWhateverItHolds() =
        runTest {
            for (error in leakyFailures) telemetry.report(BackupOp.BACKUP, error)

            assertEquals(leakyFailures.size, reporter.exceptions.size)
            for ((event, keys) in reporter.exceptions) {
                val sent = listOf(event.message.orEmpty()) + keys.orEmpty().keys + keys.orEmpty().values
                for (text in leaky) for (piece in listOf(text, "ABCDE1234F", "BAD_DECRYPT", "03/2024", "98765", "/data")) {
                    assertTrue(sent.none { piece in it }, "'$piece' reached telemetry: $sent")
                }
            }
        }

    @Test
    fun everyKeyPassesTheSanitizerAndEveryValueIsAFixedCode() =
        runTest {
            val errors = byKind() + leakyFailures + listOf<Throwable?>(null)
            for (op in BackupOp.entries) for (error in errors) for (version in listOf(null, 1, 5, 6, 7, 0, -3, 65_535)) telemetry.report(op, error, version)

            val formats = (1..PortableBackup.CURRENT_VERSION).map { it.toString() } + listOf("newer", "unknown")
            val allowedValues =
                mapOf(
                    "backup_failure_code" to BackupFailure.entries.map { it.code }.toSet(),
                    "backup_format_version" to formats.toSet(),
                    "backup_op" to BackupOp.entries.map { it.code }.toSet(),
                )
            for ((_, keys) in reporter.exceptions) {
                val sent = keys.orEmpty()
                assertEquals(allowedValues.keys, sent.keys)
                for ((key, value) in sent) {
                    assertTrue(TelemetrySanitizer.isKeyAllowed(key), "$key is dropped by the platform reporter")
                    assertTrue(value in allowedValues.getValue(key), "$key = '$value' is not a fixed code")
                }
                assertEquals(sent, TelemetrySanitizer.sanitizeMetadata(sent), "the platform's sanitizer passes every key and value unchanged")
            }
        }

    @Test
    fun theFormatSaysWhichVersionFailedOrThatItWasNewerOrUnknown() =
        runTest {
            telemetry.report(BackupOp.BACKUP, BackupVerifyException(), PortableBackup.CURRENT_VERSION)
            telemetry.report(BackupOp.RESTORE, UnsupportedBackupVersionException(65_535))
            telemetry.report(BackupOp.RESTORE, BackupDamagedException(), 4)
            telemetry.report(BackupOp.RESTORE, BackupDamagedException(), 65_535)

            assertEquals(
                listOf("6" to "verify_failed", "newer" to "newer_version", "4" to "damaged", "newer" to "damaged"),
                reporter.exceptions.map { (_, keys) -> keys!!.getValue("backup_format_version") to keys.getValue("backup_failure_code") },
            )
            assertEquals(listOf("backup", "restore", "restore", "restore"), reporter.exceptions.map { it.second!!.getValue("backup_op") })
        }

    @Test
    fun nothingIsSentWhenTheOfficerTurnedTelemetryOff() =
        runTest {
            settings.insertSettings(AppSettingsEntity(isTelemetryEnabled = false))
            for (error in byKind()) telemetry.report(BackupOp.RESTORE, error)

            assertTrue(reporter.exceptions.isEmpty() && reporter.logs.isEmpty() && reporter.keys.isEmpty())

            settings.insertSettings(AppSettingsEntity(isTelemetryEnabled = true))
            telemetry.report(BackupOp.RESTORE, BackupDamagedException())
            assertEquals(1, reporter.exceptions.size, "turned back on, the next failure is sent")
        }

    @Test
    fun aDeviceWithNoSettingsYetHasTheAppsDefaultWhichIsOn() =
        runTest {
            telemetry.report(BackupOp.RESTORE, BackupDamagedException())

            assertEquals(1, reporter.exceptions.size)
        }

    @Test
    fun nothingIsSentWhenTheChoiceCannotBeRead() =
        runTest {
            val unreadable =
                object : PayslipDao by FakePayslipDao() {
                    override suspend fun getSettings(): AppSettingsEntity? = error("settings unreadable")
                }
            BackupTelemetry(reporter, unreadable).report(BackupOp.BACKUP, BackupFileException())

            assertTrue(reporter.exceptions.isEmpty(), "an unknown choice is treated as off")
        }

    private fun byKind(): List<Throwable> =
        listOf(
            DecryptionFailedException(), NotABackupException(), UnsupportedBackupVersionException(9), BackupDamagedException(),
            BackupTooLargeException(), BackupStorageFullException(), BackupVerifyException(), BackupFileException(), IllegalStateException(),
        )
}
