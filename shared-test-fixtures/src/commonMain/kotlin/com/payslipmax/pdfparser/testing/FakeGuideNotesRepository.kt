package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.data.GuideNoteSaveResult
import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.data.StoredGuideNotes
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.guide.domain.GuideNoteEdit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [GuideNotesRepository] with the same rules as the Room one (they are [GuideNote]'s); nothing is encrypted. */
class FakeGuideNotesRepository(
    private val clock: () -> Long = { 0L },
) : GuideNotesRepository {
    private val notes = MutableStateFlow<Map<String, GuideNote>>(emptyMap())

    override fun observe(): Flow<StoredGuideNotes> = notes.map { StoredGuideNotes(it.values.sortedByDescending(GuideNote::updatedAt)) }

    override suspend fun save(
        cardId: String,
        text: String,
        cardRev: String,
    ): GuideNoteSaveResult =
        when (val edit = GuideNoteEdit.of(cardId, text, cardRev, clock())) {
            is GuideNoteEdit.Save -> {
                notes.value = notes.value + (cardId to edit.note)
                GuideNoteSaveResult.SAVED
            }
            is GuideNoteEdit.Erase -> {
                delete(edit.cardId)
                GuideNoteSaveResult.DELETED
            }
            GuideNoteEdit.Reject -> GuideNoteSaveResult.REJECTED
        }

    override suspend fun delete(cardId: String) {
        notes.value = notes.value - cardId
    }
}
