package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.PrintedRow
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts.MARKER_TEXT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * WHY: the print snapshot is the rendered text of what the officer handed the clerk, so it is as private as the draft: encrypted with it,
 * read back exactly, bounded like everything else stored (a restored file is untrusted), and optional inside payload `v: 1` so an
 * older draft, or an older app, never trips over it.
 */
class Part2PrintedCodecTest {
    private val key = "device-key-for-tests"
    private val printed = SyntheticPart2Drafts.printedSnapshot()
    private val draft = SyntheticPart2Drafts.fullDraft().copy(printed = printed)

    private fun row(
        text: String = "x",
        data: List<String> = emptyList(),
        code: String = "CODEA",
    ) = PrintedRow(code, text, "", "", data, null, null)

    private fun withRows(vararg rows: PrintedRow) = draft.copy(printed = printed.copy(rows = rows.toList()))

    @Test
    fun aPrintedDraftComesBackWithItsSnapshotExactly() {
        val back = draft.toEntity(key).toDraft(key)

        assertEquals(draft, back)
        assertEquals(printed, back.printed)
    }

    @Test
    fun aDraftNeverPrintedHasNoSnapshotAndAnOlderRowWithoutTheFieldReadsAsNeverPrinted() {
        assertNull(SyntheticPart2Drafts.fullDraft().toEntity(key).toDraft(key).printed)

        val older = """{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"requests":[],"revs":{}}"""
        val entity = Part2DraftEntity("draft-1", CryptoHelper.encrypt(older.encodeToByteArray(), key).getOrThrow())
        assertNull(entity.toDraft(key).printed)
    }

    @Test
    fun theRowStillHoldsNoReadableSnapshotText() {
        val row = draft.toEntity(key)
        val onDisk = row.id + row.ciphertext.decodeToString()

        for (secret in listOf(MARKER_TEXT, "CERTIFICATE", "Authority line", "05/07/2026")) assertFalse(onDisk.contains(secret), "$secret must not be readable")
    }

    @Test
    fun aSnapshotAtEveryLimitIsAccepted() {
        val atLimit =
            draft.copy(
                printed =
                    printed.copy(
                        rows = List(Part2Limits.PRINTED_ROWS_MAX) { row("t".repeat(Part2Limits.PRINTED_TEXT_MAX), List(Part2Limits.PRINTED_DATA_MAX) { "d" }) },
                    ),
            )

        assertEquals(null, Part2Bounds.check(atLimit))
        assertEquals(null, Part2Bounds.check(draft.copy(printed = printed.copy(at = 0))), "the epoch itself is a time")
    }

    @Test
    fun aSnapshotOverALimitIsRefusedOnTheWayInAndUnreadableOnTheWayOut() {
        val over =
            listOf(
                draft.copy(printed = printed.copy(rows = List(Part2Limits.PRINTED_ROWS_MAX + 1) { row() })),
                withRows(row("t".repeat(Part2Limits.PRINTED_TEXT_MAX + 1))),
                withRows(row().copy(from = "t".repeat(Part2Limits.PRINTED_TEXT_MAX + 1))),
                withRows(row().copy(to = "t".repeat(Part2Limits.PRINTED_TEXT_MAX + 1))),
                withRows(row().copy(authority = "t".repeat(Part2Limits.PRINTED_TEXT_MAX + 1))),
                withRows(row().copy(certificate = "t".repeat(Part2Limits.PRINTED_TEXT_MAX + 1))),
                withRows(row(data = List(Part2Limits.PRINTED_DATA_MAX + 1) { "d" })),
                withRows(row(data = listOf("t".repeat(Part2Limits.PRINTED_TEXT_MAX + 1)))),
                withRows(row(code = "BAD CODE")),
                draft.copy(printed = printed.copy(at = -1)),
                draft.copy(printed = printed.copy(revs = mapOf("CODEA" to "r".repeat(Part2Limits.REV_MAX + 1)))),
                draft.copy(printed = printed.copy(revs = mapOf("BAD CODE" to "a1"))),
                draft.copy(printed = printed.copy(revs = (0..Part2Limits.REVS_MAX).associate { "C$it" to "a" })),
            )
        for (bad in over) assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(bad))
    }

    @Test
    fun aStoredSnapshotOverALimitIsUnreadableNotTrusted() {
        val tooManyRows = List(Part2Limits.PRINTED_ROWS_MAX + 1) { """{"code":"A","narrative":"n"}""" }.joinToString(",")
        val json = """{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"printed":{"at":3,"rows":[$tooManyRows]}}"""
        val entity = Part2DraftEntity("draft-1", CryptoHelper.encrypt(json.encodeToByteArray(), key).getOrThrow())

        assertFailsWith<Part2UnreadableException> { entity.toDraft(key) }
    }
}
