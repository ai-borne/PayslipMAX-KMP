package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * WHY: a cancel slip and the re-draft made with it point at each other (DECISIONS Round 22). The pointer is a draft id, so it is
 * encrypted with the draft, read back exactly, optional inside payload `v: 1` (every older draft has none) and bounded like every id,
 * since a restored file is untrusted. The schema does not change for it.
 */
class Part2DraftPairCodecTest {
    private val key = "device-key-for-tests"
    private val paired = SyntheticPart2Drafts.fullDraft().copy(pairedWith = "draft-2")

    @Test
    fun aPairedDraftComesBackPointingAtItsPartner() {
        assertEquals(paired, paired.toEntity(key).toDraft(key))
        assertEquals("draft-2", paired.toEntity(key).toDraft(key).pairedWith)
    }

    @Test
    fun aDraftWithNoPartnerAndAnOlderRowWithoutTheFieldReadAsUnpaired() {
        assertNull(SyntheticPart2Drafts.fullDraft().toEntity(key).toDraft(key).pairedWith)

        val older = """{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"requests":[],"revs":{}}"""
        val entity = Part2DraftEntity("draft-1", CryptoHelper.encrypt(older.encodeToByteArray(), key).getOrThrow())
        assertNull(entity.toDraft(key).pairedWith)
    }

    @Test
    fun thePartnersIdIsNotReadableOnDisk() {
        val row = paired.toEntity(key)

        assertFalse((row.id + row.ciphertext.decodeToString()).contains("draft-2"))
    }

    @Test
    fun aPartnerIdAtTheLimitIsAcceptedAndAnythingThatIsNotAPlainIdIsRefused() {
        assertEquals(null, Part2Bounds.check(paired.copy(pairedWith = "a".repeat(Part2Limits.ID_MAX))))
        for (bad in listOf("", "a".repeat(Part2Limits.ID_MAX + 1), "has space", "x/../y", "é")) {
            assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(paired.copy(pairedWith = bad)), "partner id '$bad'")
        }
    }

    @Test
    fun aStoredRowWhoseStoredPartnerIdIsNotAPlainIdIsUnreadableNotTrusted() {
        val json = """{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"pair":"bad id!"}"""
        val entity = Part2DraftEntity("draft-1", CryptoHelper.encrypt(json.encodeToByteArray(), key).getOrThrow())

        kotlin.test.assertFailsWith<Part2UnreadableException> { entity.toDraft(key) }
    }
}
