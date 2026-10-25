package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * How one item (a draft, a vault order) reaches the encrypted repository: one write [PAUSE_MS] after the last change (never one per key,
 * because every save re-derives the key), at once for what must not wait (a completed print, leaving the form), never after a delete, and
 * one at a time (a [Mutex]), so a late write cannot overtake an earlier one. [item] reads the model's current item when a write is made,
 * [write] and [remove] are the model's own repository calls (so this class cannot reach storage), and [onFailureChange] says a write
 * started or stopped failing. It stores nothing itself and logs nothing.
 */
internal class AutoSaver<T>(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val item: () -> T,
    private val write: suspend (T) -> Part2WriteResult,
    private val remove: suspend () -> Unit,
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

    /** The item changed: write it after the pause, or after the next change's pause if one comes first. */
    fun changed() {
        dirty = true
        pending?.cancel()
        pending =
            scope.launch {
                delay(PAUSE_MS)
                save()
            }
    }

    /** Writes the item now, in place of any write still waiting. */
    suspend fun saveAtOnce() {
        dirty = true
        pending?.cancel()
        save()
    }

    /** Deletes the item; a write still waiting is dropped, never written back. [onDone] runs once it is gone. */
    fun delete(onDone: () -> Unit) {
        pending?.cancel()
        scope.launch {
            lock.withLock { remove() }
            onDone()
        }
    }

    /**
     * Leaving the form: returns the item to write now, or null when everything is already written. The model then cancels its scope (which
     * drops any write still waiting), so the caller passes the result to [writeAfterClose].
     */
    fun closing(): T? = if (dirty) item() else null

    /** Writes what [closing] returned, outside the model's cancelled scope. */
    fun writeAfterClose(last: T) {
        CoroutineScope(SupervisorJob() + dispatcher).launch { lock.withLock { write(last) } }
    }

    private suspend fun save() {
        val current = item()
        val result = lock.withLock { write(current) }
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
