package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * How one draft reaches the encrypted repository: one write [PAUSE_MS] after the last change (never one per key, because every save
 * re-derives the key), at once for what must not wait (a completed print, leaving the form), never after a delete, and one at a time
 * (a [Mutex]), so a late write cannot overtake an earlier one. [draft] reads the model's current draft when a write is made, and
 * [onFailureChange] says a write started or stopped failing. It stores nothing itself and logs nothing.
 */
internal class DraftSaver(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val repository: Part2Repository,
    private val draft: () -> Part2Draft,
    private val onFailureChange: () -> Unit,
) {
    private val lock = Mutex()
    private var pending: Job? = null

    /** True from a change until it is written: what leaving the form must still write. */
    var dirty = false
        private set

    /** True while the last write was refused (a cap, a full device): the screen says so. */
    var failed = false
        private set

    /** The draft changed: write it after the pause, or after the next change's pause if one comes first. */
    fun changed() {
        dirty = true
        pending?.cancel()
        pending =
            scope.launch {
                delay(PAUSE_MS)
                save()
            }
    }

    /** Writes the draft now, in place of any write still waiting. */
    suspend fun saveAtOnce() {
        dirty = true
        pending?.cancel()
        save()
    }

    /** Deletes the draft; a write still waiting is dropped, never written back. [onDone] runs once it is gone. */
    fun delete(
        id: String,
        onDone: () -> Unit,
    ) {
        pending?.cancel()
        scope.launch {
            lock.withLock { repository.deleteDraft(id) }
            onDone()
        }
    }

    /**
     * Leaving the form: returns the draft to write now, or null when everything is already written. The model then cancels its scope (which
     * drops any write still waiting), so the caller passes the result to [writeAfterClose].
     */
    fun closing(): Part2Draft? = if (dirty) draft() else null

    /** Writes what [closing] returned, outside the model's cancelled scope. */
    fun writeAfterClose(last: Part2Draft) {
        CoroutineScope(SupervisorJob() + dispatcher).launch { lock.withLock { repository.saveDraft(last) } }
    }

    private suspend fun save() {
        val current = draft()
        val result = lock.withLock { repository.saveDraft(current) }
        dirty = false
        val refused = result != Part2WriteResult.SAVED
        if (refused != failed) {
            failed = refused
            onFailureChange()
        }
    }

    companion object {
        /** The pause after the last change: long enough that typing is one write, short enough to lose little. */
        const val PAUSE_MS = 300L
    }
}
