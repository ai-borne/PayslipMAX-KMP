package com.payslipmax.pdfparser.database

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.guide.domain.GuideNote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WHY: a note is the user's private opinion, so nothing readable may reach the database file. Like a payslip correction, a row
 * holds one AES-GCM ciphertext; the card id is the key and the text, the card revision and the time are inside it.
 */
class GuideNoteEntityTest {
    private val key = "device-key-for-tests"
    private val secret = "my private TPTA doubt zq9"
    private val note = assertNotNull(GuideNote.of("RB-TD-001", secret, "a1b2c3d4", 1_700_000_000_000L))

    @Test
    fun aNoteRoundTripsThroughItsEncryptedRow() {
        val row = note.toEntity(key)

        assertEquals(note, row.toNote(key))
        assertEquals("RB-TD-001", row.cardId)
    }

    @Test
    fun theRowHoldsNoReadableTextRevisionOrTime() {
        val row = note.toEntity(key)
        val onDisk = row.cardId + row.ciphertext

        assertFalse(onDisk.contains("TPTA"), "the text must not be on disk")
        assertFalse(onDisk.contains("a1b2c3d4"), "the revision must not be on disk")
        assertFalse(onDisk.contains("1700000000000"), "the time must not be on disk")
        assertFalse(onDisk.contains("private"))
    }

    @Test
    fun theSameNoteEncryptsDifferentlyEachTime() {
        // A fresh salt and IV per write, so two identical notes cannot be told apart on disk.
        assertNotEquals(note.toEntity(key).ciphertext, note.toEntity(key).ciphertext)
    }

    @Test
    fun theWrongKeyIsUnreadableAndTheErrorSaysNothingAboutTheNote() {
        val row = note.toEntity(key)

        val error = assertFailsWith<GuideNoteUnreadableException> { row.toNote("another-device-key") }

        assertNoLeak(error)
    }

    @Test
    fun tamperedCiphertextIsUnreadable() {
        val bytes = note.toEntity(key).ciphertext.hexToByteArray()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()
        val tampered = GuideNoteEntity("RB-TD-001", bytes.toHex())

        assertFailsWith<GuideNoteUnreadableException> { tampered.toNote(key) }
    }

    @Test
    fun garbageThatIsNotHexIsUnreadableNotACrash() {
        assertFailsWith<GuideNoteUnreadableException> { GuideNoteEntity("RB-TD-001", "not hex at all").toNote(key) }
        assertFailsWith<GuideNoteUnreadableException> { GuideNoteEntity("RB-TD-001", "").toNote(key) }
    }

    @Test
    fun aRowMovedToAnotherCardsKeyIsRefused() {
        // GCM protects the ciphertext, not the primary key next to it; the id inside the ciphertext is what catches a swap.
        val moved = GuideNoteEntity("RB-TD-002", note.toEntity(key).ciphertext)

        assertFailsWith<GuideNoteUnreadableException> { moved.toNote(key) }
    }

    @Test
    fun decryptableJsonThatIsNotANoteIsUnreadableAndNeverEchoesItsContent() {
        // kotlinx.serialization quotes the input in its error text; it must not reach ours.
        val bad = """{"cardId":"RB-TD-001","text":"$secret""" // truncated JSON that still holds the secret
        val row = GuideNoteEntity("RB-TD-001", CryptoHelper.encrypt(bad.encodeToByteArray(), key).getOrThrow().toHex())

        val error = assertFailsWith<GuideNoteUnreadableException> { row.toNote(key) }

        assertNoLeak(error)
    }

    @Test
    fun aStoredNoteWithBlankTextIsUnreadableNotShownAsAnEmptyNote() {
        val blank = """{"cardId":"RB-TD-001","text":"   ","cardRev":"","updatedAt":1}"""
        val row = GuideNoteEntity("RB-TD-001", CryptoHelper.encrypt(blank.encodeToByteArray(), key).getOrThrow().toHex())

        assertFailsWith<GuideNoteUnreadableException> { row.toNote(key) }
    }

    @Test
    fun aStoredNoteOverTheCapIsCutOnReadSoARestoredFileCannotExceedIt() {
        val long = """{"cardId":"RB-TD-001","text":"${"a".repeat(5000)}","cardRev":"","updatedAt":1}"""
        val row = GuideNoteEntity("RB-TD-001", CryptoHelper.encrypt(long.encodeToByteArray(), key).getOrThrow().toHex())

        assertEquals(2000, row.toNote(key).text.length)
    }

    @Test
    fun aFieldAddedByALaterAppIsIgnoredNotFatal() {
        val future = """{"cardId":"RB-TD-001","text":"hello","cardRev":"abc","updatedAt":7,"color":"red"}"""
        val row = GuideNoteEntity("RB-TD-001", CryptoHelper.encrypt(future.encodeToByteArray(), key).getOrThrow().toHex())

        assertEquals("hello", row.toNote(key).text)
    }

    private fun assertNoLeak(error: Throwable) {
        val everything = generateSequence(error) { it.cause }.joinToString(" | ") { it.message.orEmpty() }
        assertFalse(everything.contains("TPTA") || everything.contains(secret), "the error text must not carry the note: $everything")
        assertFalse(everything.contains("RB-TD-001"), "nor the card id")
        assertTrue(error.cause == null, "a cause could carry the input text")
    }
}
