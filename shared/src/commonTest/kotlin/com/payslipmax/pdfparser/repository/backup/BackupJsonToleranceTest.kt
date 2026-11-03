package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.GuideNoteEntity
import com.payslipmax.pdfparser.database.PayslipCorrectionEntity
import com.payslipmax.pdfparser.database.toCorrectionEntity
import com.payslipmax.pdfparser.database.toCorrectionList
import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.database.toEntity
import com.payslipmax.pdfparser.database.toHex
import com.payslipmax.pdfparser.database.toNote
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.domain.TaxRegime
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.part2.data.Part2LegacyDraft
import com.payslipmax.pdfparser.part2.data.Part2RowBackup
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.createMockPayslip
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY (F2): a backup made by a later app version must still restore here, and the device must still read a row a later version wrote
 * before the officer rolled back. Before P2 the payslip payload was read with a strict decoder, so ONE field added by a later version
 * failed the whole restore; the outer document, corrections, notes and Part II rows each had their own, differently lenient decoder.
 * Every reader now tolerates what a later writer may do: add a key, write null where a default exists, leave a nullable field out, or
 * use an enum value this version does not know.
 */
class BackupJsonToleranceTest {
    private val later = JsonPrimitive("written by a later version")

    private fun JsonElement.withUnknownKey(): JsonObject = JsonObject(jsonObject + ("fieldFromALaterVersion" to later))

    private fun JsonObject.with(vararg changes: Pair<String, JsonElement>): JsonObject = JsonObject(this + changes)

    private fun JsonObject.without(key: String): JsonObject = JsonObject(this - key)

    private fun payslipJson(dateStr: String): JsonObject = Json.encodeToJsonElement(ParsedPayslip.serializer(), createMockPayslip(dateStr)).jsonObject

    private fun sealed(
        json: JsonElement,
        key: String,
    ): String = CryptoHelper.encrypt(json.toString().encodeToByteArray(), key).getOrThrow().toHex()

    /** A payslip payload as a later version might write it: a key this one does not know, at the top and inside a nested object. */
    private fun laterPayslip(dateStr: String): JsonObject {
        val payslip = payslipJson(dateStr)
        return payslip.with("officer" to payslip.getValue("officer").withUnknownKey()).withUnknownKey()
    }

    private suspend fun restoreDocument(document: JsonObject): FakePayslipDao {
        val dao = FakePayslipDao()
        val archive = CryptoHelper.encrypt(document.toString().encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()
        PayslipBackupService(dao, Dispatchers.Unconfined).restore(archive, BACKUP_PASSWORD).getOrThrow()
        return dao
    }

    private fun payslipRow(payload: JsonObject): JsonObject =
        JsonObject(
            mapOf(
                "dateStr" to JsonPrimitive("08/2024"),
                "year" to JsonPrimitive(2024),
                "monthNum" to JsonPrimitive(8),
                "monthName" to JsonPrimitive("August"),
                "ciphertext" to JsonPrimitive(sealed(payload, BACKUP_PASSWORD)),
            ),
        )

    @Test
    fun aBackupWhoseEveryRowAndPayslipCarriesAFieldFromALaterVersionRestoresInFull() =
        runTest {
            val row = { json: JsonObject -> JsonArray(listOf(json.withUnknownKey())) }
            val document =
                JsonObject(
                    mapOf(
                        "version" to JsonPrimitive(5),
                        "encryptedPayslips" to row(payslipRow(laterPayslip("08/2024"))),
                        "pdfs" to row(Json.encodeToJsonElement(PdfBackupRow.serializer(), PdfBackupRow("08/2024", byteArrayOf(9))).jsonObject),
                        "settings" to Json.encodeToJsonElement(SettingsBackup.serializer(), SettingsBackup(appTheme = "dark")).withUnknownKey(),
                        "drafts" to row(Json.encodeToJsonElement(DraftBackupRow.serializer(), DraftBackupRow("d1", "08/2024", "T", "R", "S", "B", 1)).jsonObject),
                        "dismissedDrafts" to row(Json.encodeToJsonElement(DismissedDraftBackupRow.serializer(), DismissedDraftBackupRow("08/2024", "T")).jsonObject),
                        "corrections" to row(Json.encodeToJsonElement(CorrectionBackupRow.serializer(), correctionRow()).jsonObject),
                        "guideNotes" to row(Json.encodeToJsonElement(GuideNoteBackupRow.serializer(), noteRow()).jsonObject),
                        // No Part II wiring in this service, so these rows are only decoded, never opened: decoding is what is tested.
                        "part2Entries" to row(Json.encodeToJsonElement(Part2RowBackup.serializer(), Part2RowBackup("entry-1", "x")).jsonObject),
                        "part2Orders" to row(Json.encodeToJsonElement(Part2RowBackup.serializer(), Part2RowBackup("order-1", "x")).jsonObject),
                        "part2Drafts" to row(Json.encodeToJsonElement(Part2LegacyDraft.serializer(), Part2LegacyDraft("draft-1")).jsonObject),
                    ),
                ).withUnknownKey()

            val dao = restoreDocument(document)

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals(createMockPayslip("08/2024"), dao.getAllPayslips().first().single().toDomain(), "every field it knows is kept")
            assertEquals(1, dao.getAllPdfs().size)
            assertEquals("dark", dao.getSettings()?.appTheme)
            assertEquals(1, dao.getAllRepresentationDrafts().first().size)
            assertEquals(1, dao.getAllDismissedDrafts().size)
            assertEquals(1, dao.getAllCorrections().first().size)
            assertEquals(1, dao.getAllGuideNotes().first().size)
        }

    private fun correctionRow() =
        CorrectionBackupRow(
            "08/2024",
            listOf(SingleCorrection("basicPay", "BPAY", 1.0, EntryCategory.EARNING, CorrectionType.EDITED, null, null, 5L))
                .toCorrectionEntity("08/2024", BACKUP_PASSWORD)
                .ciphertext,
        )

    private fun noteRow() = GuideNoteBackupRow("RB-FUTURE-1", GuideNote.of("RB-FUTURE-1", "kept", "rev00001", 1L)!!.toEntity(BACKUP_PASSWORD).ciphertext)

    @Test
    fun theDeviceReadsAPayslipRowThatALaterVersionWroteBeforeTheOfficerRolledBack() {
        val row = EncryptedPayslipEntity("08/2024", 2024, 8, "August", sealed(laterPayslip("08/2024"), CryptoHelper.getDatabaseSecretKey()))

        assertEquals(createMockPayslip("08/2024"), row.toDomain())
    }

    @Test
    fun nullWhereADefaultExistsAndAnEnumValueThisVersionDoesNotKnowTakeTheDefault() =
        runTest {
            val payslip = payslipJson("08/2024")
            val tax = payslip.getValue("taxAndSavings").jsonObject.with("taxRegime" to JsonPrimitive("A_REGIME_FROM_A_LATER_VERSION"))
            val payload = payslip.with("rawEarnings" to JsonNull, "needsReview" to JsonNull, "taxAndSavings" to tax)
            val nullLists = listOf("drafts", "dismissedDrafts", "corrections", "guideNotes", "part2Entries", "part2Orders", "part2Drafts")
            val document =
                JsonObject(
                    mapOf(
                        "version" to JsonPrimitive(5),
                        "encryptedPayslips" to JsonArray(listOf(payslipRow(payload))),
                        "pdfs" to JsonArray(emptyList()),
                        "settings" to JsonNull,
                    ) + nullLists.associateWith { JsonNull },
                )

            val restored = restoreDocument(document).getAllPayslips().first().single().toDomain()

            assertEquals(TaxRegime.OLD, restored.taxAndSavings?.taxRegime)
            assertEquals(emptyMap(), restored.rawEarnings)
            assertEquals(false, restored.needsReview)
        }

    @Test
    fun aNullableFieldALaterVersionLeavesOutReadsAsNull() =
        runTest {
            val payload = payslipJson("08/2024").without("taxAndSavings").without("diagnosticSuggestion")
            val document =
                JsonObject(
                    mapOf(
                        "version" to JsonPrimitive(5),
                        "encryptedPayslips" to JsonArray(listOf(payslipRow(payload))),
                        "pdfs" to JsonArray(emptyList()),
                    ),
                )

            assertNull(restoreDocument(document).getAllPayslips().first().single().toDomain().taxAndSavings)
        }

    @Test
    fun correctionsWithoutTheNullableOriginalsAndWithANewFieldStillRead() {
        val json = """[{"fieldKey":"basicPay","codeHead":"BPAY","amount":61000.0,"category":"EARNING","type":"EDITED","timestamp":5,"reason":"later"}]"""
        val row = PayslipCorrectionEntity("08/2024", sealed(Json.parseToJsonElement(json), BACKUP_PASSWORD))

        val correction = row.toCorrectionList(BACKUP_PASSWORD).single()

        assertEquals(61000.0, correction.amount)
        assertNull(correction.originalAmount)
        assertNull(correction.originalCodeHead)
    }

    @Test
    fun aGuideNoteWithAFieldFromALaterVersionStillReads() {
        val json = """{"cardId":"RB-FUTURE-1","text":"kept","cardRev":"rev00001","updatedAt":1,"pinned":true}"""
        val row = GuideNoteEntity("RB-FUTURE-1", sealed(Json.parseToJsonElement(json), BACKUP_PASSWORD))

        val note = row.toNote(BACKUP_PASSWORD)

        assertEquals("kept", note.text)
        assertEquals("rev00001", note.cardRev)
    }
}
