package com.payslipmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.guide.domain.GuideNote
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A personal Claim Guide note, stored the way [PayslipCorrectionEntity] stores corrections: one row per card holding an
 * AES-256-GCM ciphertext made with [CryptoHelper], so no note text, revision or time is readable in the database file.
 * The card id is the key and the only plaintext (it says which card was annotated, never what was written).
 */
@Serializable
@Entity(tableName = "guide_notes")
data class GuideNoteEntity(
    @PrimaryKey val cardId: String,
    // AES-256 encrypted JSON of GuideNotePayload (Hex-encoded)
    val ciphertext: String,
)

/**
 * A row that cannot be turned back into a note: wrong key, damaged or tampered bytes, JSON that is not a note, or a note
 * moved to another card's key. The message is fixed and there is no cause on purpose: serialization errors quote the input,
 * and the input is the user's private text.
 */
class GuideNoteUnreadableException : Exception("A Guide note could not be read.")

// The card id is repeated inside the ciphertext: GCM authenticates the ciphertext but not the primary key beside it, so
// without it a row could be moved to another card's key and still decrypt.
@Serializable
private class GuideNotePayload(
    val cardId: String,
    val text: String,
    val cardRev: String,
    val updatedAt: Long,
)

private val payloadJson = Json { ignoreUnknownKeys = true }

fun GuideNote.toEntity(password: String = CryptoHelper.getDatabaseSecretKey()): GuideNoteEntity {
    val json = Json.encodeToString(GuideNotePayload(cardId, text, cardRev, updatedAt))
    val encrypted = CryptoHelper.encrypt(json.encodeToByteArray(), password).getOrThrow()
    return GuideNoteEntity(cardId = cardId, ciphertext = encrypted.toHex())
}

/** The note this row holds, or [GuideNoteUnreadableException]. Goes through [GuideNote.of], so the cap and id rules apply to stored data too. */
fun GuideNoteEntity.toNote(password: String = CryptoHelper.getDatabaseSecretKey()): GuideNote {
    // Every failure below becomes the same fixed exception without a cause; see GuideNoteUnreadableException.
    val note =
        try {
            val bytes = CryptoHelper.decrypt(ciphertext.hexToByteArray(), password).getOrThrow()
            val payload = payloadJson.decodeFromString<GuideNotePayload>(bytes.decodeToString())
            if (payload.cardId != cardId) null else GuideNote.of(payload.cardId, payload.text, payload.cardRev, payload.updatedAt)
        } catch (e: Exception) {
            null
        }
    return note ?: throw GuideNoteUnreadableException()
}
