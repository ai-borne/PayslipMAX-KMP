package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.getLegacyFallbackKey
import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Backup v3: everything the officer *created* travels (letters with their edits, deleted-letter records,
 * per-field corrections); everything derived from payslips (ledger, insights) does not and is rebuilt.
 * A backup is also the only copy of this data if the phone is lost, so older files must keep restoring.
 */
class PayslipBackupServiceV3Test {
    private lateinit var source: FakePayslipDao
    private lateinit var target: FakePayslipDao
    private lateinit var sourceService: PayslipBackupService
    private lateinit var targetService: PayslipBackupService

    private val editedLetter =
        RepresentationDraftEntity("d-edited", "01/2023", "MISSING_HRA", "PCDA_O_PUNE", "Edited subject", "Edited body the officer wrote", 5L)
    private val correction =
        SingleCorrection("basicPay", "BPAY", 61000.0, EntryCategory.EARNING, CorrectionType.EDITED, 60000.0, "BPAY", 42L)

    @BeforeTest
    fun setUp() {
        source = FakePayslipDao()
        target = FakePayslipDao()
        sourceService = PayslipBackupService(source, Dispatchers.Unconfined)
        targetService = PayslipBackupService(target, Dispatchers.Unconfined)
    }

    private suspend fun seedSourceWithCreatedData() {
        source.insertPayslip(createMockPayslip("01/2023").toEncryptedEntity())
        source.insertRepresentationDraft(editedLetter)
        source.insertDismissedDraft(DismissedDraftEntity("01/2023", "SALARY_DROP"))
        source.insertCorrection(listOf(correction).toCorrectionEntity("01/2023"))
        // Derived rows: must never be exported.
        source.insertLedgerRecord(seedLedger("01/2023"))
    }

    private fun seedLedger(dateStr: String) =
        LedgerRecordEntity(
            dateStr, dateStr.substringAfter('/').toInt(), dateStr.substringBefore('/').toInt(),
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
        )

    private suspend fun decodeArchive(archive: ByteArray): PortableBackup {
        val json = CryptoHelper.decrypt(archive, BACKUP_PASSWORD).getOrThrow().decodeToString()
        return Json.decodeFromString(PortableBackup.serializer(), json)
    }

    @Test
    fun lettersDeletedLetterRecordsAndCorrectionsSurviveAnExportAndRestore() =
        runTest {
            seedSourceWithCreatedData()

            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(listOf(editedLetter), target.getAllRepresentationDrafts().first(), "the officer's edited text must survive")
            assertEquals(listOf(DismissedDraftEntity("01/2023", "SALARY_DROP")), target.getAllDismissedDrafts())
            val restored = target.getCorrectionByDate("01/2023")!!.toCorrectionList()
            assertEquals(listOf(correction), restored)
        }

    @Test
    fun exportIsVersion3AndNeverCarriesDerivedLedgerOrInsights() =
        runTest {
            seedSourceWithCreatedData()
            source.insertFinancialInsight(FinancialInsightEntity("i1", "01/2023", "TAX", "t", "c", "INFO", 1L))

            val backup = decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow())

            assertEquals(3, backup.version)
            assertEquals(listOf(editedLetter), backup.drafts)
            assertEquals(1, backup.dismissedDrafts.size)
            assertEquals(1, backup.corrections.size)
            val archiveText = Json.encodeToString(PortableBackup.serializer(), backup)
            assertFalse(archiveText.contains("ledger", ignoreCase = true), "ledger is derived data and must not be stored")
            assertFalse(archiveText.contains("insight", ignoreCase = true), "insights are derived data and must not be stored")
        }

    @Test
    fun correctionsInTheArchiveAreEncryptedWithTheBackupPasswordNotTheDeviceKey() =
        runTest {
            seedSourceWithCreatedData()

            val backup = decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow())

            val stored = backup.corrections.single()
            assertEquals(listOf(correction), stored.toCorrectionList(BACKUP_PASSWORD))
            val underDeviceKey = CryptoHelper.decrypt(stored.ciphertext.hexToByteArray(), CryptoHelper.getDatabaseSecretKey())
            assertTrue(underDeviceKey.isFailure, "an archive must not depend on one device's key")
        }

    @Test
    fun restoredCorrectionsAreReEncryptedUnderTheRestoringDevicesKey() =
        runTest {
            seedSourceWithCreatedData()
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()

            targetService.restore(archive, BACKUP_PASSWORD).getOrThrow()

            val bytes = target.getCorrectionByDate("01/2023")!!.ciphertext.hexToByteArray()
            assertTrue(CryptoHelper.decrypt(bytes, CryptoHelper.getDatabaseSecretKey()).isSuccess)
            assertTrue(CryptoHelper.decrypt(bytes, BACKUP_PASSWORD).isFailure)
        }

    @Test
    fun aVersion2BackupStillRestoresWithNoLettersOrCorrections() =
        runTest {
            val v2 =
                buildJsonObject {
                    put("version", 2)
                    put(
                        "encryptedPayslips",
                        Json.encodeToJsonElement(ListSerializer(EncryptedPayslipEntity.serializer()), listOf(goodEntity("08/2024"))),
                    )
                    put("pdfs", Json.encodeToJsonElement(ListSerializer(PayslipPdfEntity.serializer()), emptyList()))
                    put("settings", JsonNull)
                }
            target.seedDeviceMonth("01/2023")

            targetService.restore(encrypted(v2.toString()), BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(listOf("08/2024"), target.getAllPayslips().first().map { it.dateStr })
            assertTrue(target.getAllRepresentationDrafts().first().isEmpty())
            assertTrue(target.getAllDismissedDrafts().isEmpty())
            assertTrue(target.getAllCorrections().first().isEmpty())
        }

    @Test
    fun aVersion1BackupEncryptedWithTheLegacyKeyStillRestores() =
        runTest {
            val legacyPayslip = createMockPayslip("08/2024").toEncryptedEntity(CryptoHelper.getLegacyFallbackKey())
            // A v1 file has no "version" key at all (it was the default) and no letter or correction lists.
            val v1 =
                buildJsonObject {
                    put("encryptedPayslips", Json.encodeToJsonElement(ListSerializer(EncryptedPayslipEntity.serializer()), listOf(legacyPayslip)))
                    put("pdfs", Json.encodeToJsonElement(ListSerializer(PayslipPdfEntity.serializer()), emptyList()))
                    put("settings", JsonNull)
                }

            targetService.restore(encrypted(v1.toString()), BACKUP_PASSWORD).getOrThrow()

            assertEquals(listOf("08/2024"), target.getAllPayslips().first().map { it.dateStr })
            assertTrue(target.getAllRepresentationDrafts().first().isEmpty())
            assertTrue(target.getAllCorrections().first().isEmpty())
        }

    @Test
    fun fieldsAddedByALaterVersionDoNotBreakARestore() =
        runTest {
            val future =
                buildJsonObject {
                    put("version", 4)
                    put(
                        "encryptedPayslips",
                        Json.encodeToJsonElement(ListSerializer(EncryptedPayslipEntity.serializer()), listOf(goodEntity("08/2024"))),
                    )
                    put("pdfs", Json.encodeToJsonElement(ListSerializer(PayslipPdfEntity.serializer()), emptyList()))
                    put("settings", JsonNull)
                    put("somethingFromTheFuture", "x")
                }

            val result = targetService.restore(encrypted(future.toString()), BACKUP_PASSWORD)

            assertTrue(result.isSuccess, "unknown keys must be ignored, not fatal: ${result.exceptionOrNull()}")
            assertEquals(listOf("08/2024"), target.getAllPayslips().first().map { it.dateStr })
        }

    @Test
    fun mergeLetsTheBackupWinOnTheSameKeyAndKeepsEverythingElse() =
        runTest {
            seedSourceWithCreatedData()
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.insertRepresentationDraft(editedLetter.copy(subject = "Device version", bodyText = "Device body"))
            target.insertRepresentationDraft(editedLetter.copy(id = "d-device-only"))
            target.insertDismissedDraft(DismissedDraftEntity("02/2023", "MISSING_TPTA"))
            target.insertCorrection(listOf(correction.copy(amount = 1.0)).toCorrectionEntity("01/2023"))
            target.insertCorrection(listOf(correction.copy(amount = 2.0)).toCorrectionEntity("02/2023"))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            val letters = target.getAllRepresentationDrafts().first().associateBy { it.id }
            assertEquals(setOf("d-edited", "d-device-only"), letters.keys)
            assertEquals("Edited body the officer wrote", letters.getValue("d-edited").bodyText, "backup wins on the same id")
            assertEquals(
                setOf(DismissedDraftEntity("01/2023", "SALARY_DROP"), DismissedDraftEntity("02/2023", "MISSING_TPTA")),
                target.getAllDismissedDrafts().toSet(),
            )
            assertEquals(61000.0, target.getCorrectionByDate("01/2023")!!.toCorrectionList().single().amount)
            assertEquals(2.0, target.getCorrectionByDate("02/2023")!!.toCorrectionList().single().amount)
        }

    @Test
    fun anUnreadableCorrectionInTheBackupChangesNothingOnTheDevice() =
        runTest {
            target.seedDeviceMonth("01/2023")
            val corrupt = PayslipCorrectionEntity("03/2024", ciphertext = "deadbeef")
            val backup =
                backupBytes(PortableBackup(3, listOf(goodEntity("08/2024")), emptyList(), null, listOf(editedLetter), emptyList(), listOf(corrupt)))

            val result = targetService.restore(backup, BACKUP_PASSWORD, RestoreMode.REPLACE)

            assertTrue(result.isFailure)
            assertEquals(listOf("01/2023"), target.getAllPayslips().first().map { it.dateStr })
            assertEquals(listOf("d-01/2023"), target.getAllRepresentationDrafts().first().map { it.id })
            assertEquals(1, target.getAllCorrections().first().size)
        }

    @Test
    fun anUndecryptableCorrectionOnTheDeviceIsSkippedNotFatalToTheBackup() =
        runTest {
            seedSourceWithCreatedData()
            source.insertCorrection(PayslipCorrectionEntity("05/2023", ciphertext = "deadbeef"))

            val backup = decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow())

            assertEquals(listOf("01/2023"), backup.corrections.map { it.dateStr })
        }

    private fun encrypted(json: String) = CryptoHelper.encrypt(json.encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()
}
