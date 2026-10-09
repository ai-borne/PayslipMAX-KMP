package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.GuideNoteUnreadableException
import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.database.toEntity
import com.payslipmax.pdfparser.database.toNote
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.guide.domain.GuideNoteEdit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * [GuideNotesRepository] over the encrypted `guide_notes` table. Each save is one Room statement, so a process kill leaves
 * the old note or the new one, never half. Encryption and decryption run on [dispatcher], off the main thread.
 */
class RoomGuideNotesRepository(
    private val dao: PayslipDao,
    private val key: () -> String = { CryptoHelper.getDatabaseSecretKey() },
    private val clock: () -> Long = { CryptoHelper.getCurrentTimeMillis() },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : GuideNotesRepository {
    override fun observe(): Flow<StoredGuideNotes> =
        dao
            .getAllGuideNotes()
            .map { rows ->
                val deviceKey = key()
                val readable = mutableListOf<GuideNote>()
                var unreadable = 0
                for (row in rows) {
                    try {
                        readable += row.toNote(deviceKey)
                    } catch (e: GuideNoteUnreadableException) {
                        unreadable++
                    }
                }
                StoredGuideNotes(readable.sortedByDescending { it.updatedAt }, unreadable)
            }.flowOn(dispatcher)

    override suspend fun save(
        cardId: String,
        text: String,
        cardRev: String,
    ): GuideNoteSaveResult =
        withContext(dispatcher) {
            when (val edit = GuideNoteEdit.of(cardId, text, cardRev, clock())) {
                is GuideNoteEdit.Save -> {
                    dao.insertGuideNote(edit.note.toEntity(key()))
                    GuideNoteSaveResult.SAVED
                }
                is GuideNoteEdit.Erase -> {
                    dao.deleteGuideNote(edit.cardId)
                    GuideNoteSaveResult.DELETED
                }
                GuideNoteEdit.Reject -> GuideNoteSaveResult.REJECTED
            }
        }

    override suspend fun delete(cardId: String) = withContext(dispatcher) { dao.deleteGuideNote(cardId) }
}
