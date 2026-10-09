package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.domain.GuideNote
import kotlinx.coroutines.flow.Flow

/** What a save did: wrote the note, erased it (blank text), or refused it (the card id or revision is not a plain token). */
enum class GuideNoteSaveResult { SAVED, DELETED, REJECTED }

/**
 * The notes the device holds. [unreadable] counts rows that could not be decrypted (for example after the device key was
 * lost): they are kept on disk, never shown, and never deleted behind the user's back, so a screen can say "N notes could not be read".
 */
class StoredGuideNotes(
    val notes: List<GuideNote>,
    val unreadable: Int = 0,
) {
    // The same rule as GuideNote: a note's text must never reach a log through a string template.
    override fun toString(): String = "StoredGuideNotes(${notes.size}, unreadable=$unreadable)"
}

/**
 * The user's private Claim Guide notes. They stay on the device, encrypted, and leave it only inside the user's own `.pcda`
 * backup. All rules (plain card id, trim, 2000-character cap, blank means delete) are [GuideNote]'s; an implementation
 * cannot store a note that breaks them.
 */
interface GuideNotesRepository {
    /** Every readable note, emitting again after each change. Collect it from a ViewModel scope. */
    fun observe(): Flow<StoredGuideNotes>

    /** Saves [text] as the note for [cardId], written against the card revision [cardRev]; blank [text] deletes the note. */
    suspend fun save(
        cardId: String,
        text: String,
        cardRev: String,
    ): GuideNoteSaveResult

    suspend fun delete(cardId: String)
}
