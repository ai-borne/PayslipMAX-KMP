package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.data.GuideNoteSaveResult
import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.data.StoredGuideNotes
import com.payslipmax.pdfparser.guide.domain.GuideNotePlacements
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

/** What a save or an edit did. [SAVED_NOT_MOVED]: the text is saved, but the earlier copy it should replace is still there. */
enum class GuideNoteOutcome { SAVED, SAVED_NOT_MOVED, DELETED, FAILED }

/**
 * App-scoped (a Koin single) holder of the user's personal notes for the Guide, kept apart from [GuideViewModel]. It places the
 * stored notes against the loaded bundle and writes edits through [GuideNotesRepository], which owns the rules (a plain card
 * id, 2000 characters, blank means delete). It tells telemetry nothing: it has no crash reporter, and a failed write is
 * returned to the screen as an outcome. Note text lives in [state] in memory only; the editor's draft is not here (it is
 * the dialog's, so it is gone on cancel, tab switch, lock and process death).
 */
class GuideNotesViewModel(
    private val repository: GuideNotesRepository,
    private val guide: GuideViewModel,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /**
     * Read from the store only while a screen collects it (so a locked card screen, which never collects, never decrypts a
     * note). Until the Guide has loaded no card is known, so nothing is placed (placing notes against no cards would call every one of
     * them "on a removed card"). A store that fails to read shows no notes rather than ending the flow with an error.
     */
    val state: StateFlow<GuideNotesState> =
        combine(repository.observe().catch { emit(StoredGuideNotes(emptyList())) }, guide.uiState) { stored, ui ->
            if (ui is GuideUiState.Ready) GuideNotesState(GuideNotePlacements.of(stored.notes, ui.bundle.cards), stored.unreadable) else GuideNotesState.None
        }.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GuideNotesState.None)

    /**
     * Saves [text] as the note on [cardId], against that card's current revision. When [movedFrom] names the earlier rule's
     * note being edited on the current card, that row is deleted afterwards: the text is saved first, so if the delete fails
     * nothing is lost and the outcome says the earlier copy remains. Runs in this model's scope, so leaving the screen does not cancel it.
     */
    fun save(
        cardId: String,
        text: String,
        movedFrom: String? = null,
        onDone: (GuideNoteOutcome) -> Unit = {},
    ) {
        scope.launch { onDone(attempt { write(cardId, text, movedFrom) } ?: GuideNoteOutcome.FAILED) }
    }

    /** Deletes the note stored under [sourceCardId]; [onDone] gets false when the store failed. */
    fun delete(
        sourceCardId: String,
        onDone: (Boolean) -> Unit = {},
    ) {
        scope.launch { onDone(attempt { repository.delete(sourceCardId) } != null) }
    }

    private suspend fun write(
        cardId: String,
        text: String,
        movedFrom: String?,
    ): GuideNoteOutcome {
        val rev = guide.cardRev(cardId) ?: return GuideNoteOutcome.FAILED
        return when (repository.save(cardId, text, rev)) {
            GuideNoteSaveResult.SAVED -> if (movedFrom == null || movedFrom == cardId || removed(movedFrom)) GuideNoteOutcome.SAVED else GuideNoteOutcome.SAVED_NOT_MOVED
            GuideNoteSaveResult.DELETED -> GuideNoteOutcome.DELETED
            GuideNoteSaveResult.REJECTED -> GuideNoteOutcome.FAILED
        }
    }

    private suspend fun removed(cardId: String): Boolean = attempt { repository.delete(cardId) } != null

    // A store error is the screen's to report: it is never logged here, because its message could quote a note.
    private suspend fun <T : Any> attempt(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
