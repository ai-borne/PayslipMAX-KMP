package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * WHY: the reminders an officer dismisses are remembered with the draft (DECISIONS Round 26), so they must be encrypted with it, read back
 * exactly, optional inside payload `v: 1` (every older draft has none) and bounded like every stored list, since a restored file is untrusted.
 * The schema does not change for it.
 */
class Part2DraftDismissedCodecTest {
    private val key = "device-key-for-tests"
    private val dismissed = SyntheticPart2Drafts.fullDraft().copy(dismissed = setOf("return", "year-end-2026"))

    @Test
    fun dismissedRemindersComeBackExactly() {
        assertEquals(dismissed, dismissed.toEntity(key).toDraft(key))
        assertEquals(setOf("return", "year-end-2026"), dismissed.toEntity(key).toDraft(key).dismissed)
    }

    @Test
    fun aDraftWithNoneAndAnOlderRowWithoutTheFieldReadAsNothingDismissed() {
        assertEquals(emptySet(), SyntheticPart2Drafts.fullDraft().copy(dismissed = emptySet()).toEntity(key).toDraft(key).dismissed)

        val older = """{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"requests":[],"revs":{}}"""
        val entity = Part2DraftEntity("draft-1", CryptoHelper.encrypt(older.encodeToByteArray(), key).getOrThrow())
        assertEquals(emptySet(), entity.toDraft(key).dismissed)
    }

    @Test
    fun theNamesAreNotReadableOnDisk() {
        val row = dismissed.toEntity(key)

        assertFalse((row.id + row.ciphertext.decodeToString()).contains("year-end-2026"))
    }

    @Test
    fun theCountAtTheLimitIsAcceptedAndOneMoreIsRefused() {
        fun many(n: Int) = dismissed.copy(dismissed = (1..n).map { "t$it" }.toSet())
        assertEquals(null, Part2Bounds.check(many(Part2Limits.DISMISSED_MAX)))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(many(Part2Limits.DISMISSED_MAX + 1)))
    }

    @Test
    fun aNameIsAPlainTokenWithinTheAnswerLengthOrRefused() {
        assertEquals(null, Part2Bounds.check(dismissed.copy(dismissed = setOf("a".repeat(Part2Limits.ANSWER_MAX)))))
        for (bad in listOf("", "a".repeat(Part2Limits.ANSWER_MAX + 1), "has space", "x/../y", "<b>", "é", "a:b")) {
            assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(dismissed.copy(dismissed = setOf(bad))), "name '$bad'")
        }
    }

    @Test
    fun aStoredRowWithAnImpossibleNameOrTooManyIsUnreadableNotTrusted() {
        val bad = """{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"dismissed":["bad name!"]}"""
        assertFailsWith<Part2UnreadableException> { Part2DraftEntity("draft-1", CryptoHelper.encrypt(bad.encodeToByteArray(), key).getOrThrow()).toDraft(key) }

        val names = (1..Part2Limits.DISMISSED_MAX + 1).joinToString(",") { "\"t$it\"" }
        val surplus = """{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"dismissed":[$names]}"""
        assertFailsWith<Part2UnreadableException> { Part2DraftEntity("draft-1", CryptoHelper.encrypt(surplus.encodeToByteArray(), key).getOrThrow()).toDraft(key) }
    }
}
