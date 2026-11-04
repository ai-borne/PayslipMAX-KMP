package com.payslipmax.pdfparser.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.part2.data.LastBackupStorage
import com.payslipmax.pdfparser.repository.BackupFiles
import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.repository.BackupTelemetry
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.PreparedRestore
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.ui.screens.BackupContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The Backup & Restore sheet's view model (docs/Plan/14a P7): the sheet draws [state] and calls these, nothing more. It is one instance per
 * process (a Koin single), so a backup or restore runs on in [viewModelScope] when the sheet closes, Settings leaves the screen or the app
 * locks, and its result is there when the sheet opens again.
 * - The password is passed per call and kept nowhere: not in [state], not in a field.
 * - A restore stops at [BackupUiState.Preview] with the picked file open and the archive's key held ([PreparedRestore]). Every way out (a
 *   commit that works or fails, [cancel], [dismiss], [onCleared]) closes it and deletes the file.
 * - One operation at a time: Back Up or Restore while one is working or previewed is ignored, and an ignored pick is deleted.
 * - A failure is kept as its code ([BackupUiState.Failed.of]) and sent once, code-only, through [telemetry].
 */
class BackupRestoreViewModel(
    internal val service: PayslipBackupService,
    private val files: BackupFiles,
    internal val telemetry: BackupTelemetry,
    private val lastBackup: LastBackupStorage,
) : ViewModel() {
    private val _state = MutableStateFlow<BackupUiState>(idle())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private val throttle = ProgressThrottle(viewModelScope) { _state.value = it }

    // Which operation owns the state: a preview that was cancelled must not overwrite what came after it.
    private var generation = 0
    private var pending: PendingRestore? = null

    /** The prepared restore the preview on screen holds, for tests that check it is closed. */
    internal val heldRestore: PreparedRestore? get() = pending?.prepared

    /** The Orders Bank entries and orders a backup would carry (Settings shows Bank-only devices as not empty). */
    suspend fun part2ItemCount(): Int = service.part2ItemCount()

    /** Writes the backup, reads it back and only then opens the share sheet on it. [contents] is what the result line reports. */
    fun backUp(
        password: String,
        contents: BackupContents,
    ) {
        val op = begin(BackupOp.BACKUP) ?: return
        viewModelScope.launch {
            val result = service.backUp(files, password, throttle.progress(BackupOp.BACKUP)).mapCatching { files.share().getOrThrow() }
            end(op, BackupOp.BACKUP, result, PortableBackup.CURRENT_VERSION) {
                BackupUiState.Done(BackupSummary.BackedUp(contents.payslips, contents.part2Items))
            }
        }
    }

    /** Opens and checks the [picked] file, writing nothing, and waits at the preview for [commit] or [cancel]. The file is always deleted. */
    fun restore(
        picked: String,
        password: String,
    ) {
        val op = begin(BackupOp.RESTORE) ?: return files.discard(picked)
        viewModelScope.launch {
            val progress = throttle.progress(BackupOp.RESTORE)
            var format: Int? = null
            val result =
                files.read(picked) { source ->
                    val prepared = service.prepare(source, password, progress).getOrElse { return@read Result.failure(it) }
                    prepared.use { held ->
                        val mode = awaitChoice(picked, held) ?: return@read Result.success(null)
                        format = held.preview.formatVersion
                        service.commit(held, mode, progress)
                    }
                }
            end(op, BackupOp.RESTORE, result, format) { report -> report?.let { BackupUiState.Done(BackupSummary.Restored(it.skipped)) } ?: idle() }
        }
    }

    /** Writes the previewed backup ([mode]: Replace or Merge). Ignored unless a preview is on screen. */
    fun commit(mode: RestoreMode) {
        val choice = pending ?: return
        pending = null
        throttle.start(BackupUiState.Working(BackupOp.RESTORE, BackupStage.PAGES, 0, 0))
        choice.answer.complete(mode)
    }

    /** Leaves the preview without writing anything: the prepared restore is closed and the picked file deleted. */
    fun cancel() {
        val choice = pending ?: return
        release(choice)
        generation++
        _state.value = idle()
        choice.answer.complete(null)
    }

    /** The sheet was closed: a preview is cancelled and a result is put away. Work in progress carries on (no Cancel while working, D12). */
    fun dismiss() {
        when (_state.value) {
            is BackupUiState.Preview -> cancel()
            is BackupUiState.Done, is BackupUiState.Failed -> _state.value = idle()
            is BackupUiState.Idle, is BackupUiState.Working -> Unit
        }
    }

    /** Its owner is gone: the key is wiped and the file deleted now, since nothing runs after this to unwind the waiting restore. */
    override fun onCleared() {
        pending?.let(::release)
    }

    private fun idle() = BackupUiState.Idle(lastBackup.load())

    /** Starts [op] unless one is working or previewed; returns the operation's number, or null when the tap is ignored. */
    private fun begin(op: BackupOp): Int? {
        if (_state.value is BackupUiState.Working || _state.value is BackupUiState.Preview) return null
        throttle.start(BackupUiState.Working(op, BackupStage.OPENING, 0, 0))
        return ++generation
    }

    private suspend fun awaitChoice(
        picked: String,
        prepared: PreparedRestore,
    ): RestoreMode? {
        val answer = CompletableDeferred<RestoreMode?>()
        pending = PendingRestore(picked, prepared, answer)
        throttle.stop()
        _state.value = BackupUiState.Preview(prepared.preview)
        return answer.await()
    }

    private fun release(choice: PendingRestore) {
        pending = null
        choice.prepared.close()
        files.discard(choice.picked)
    }

    private suspend fun <T> end(
        op: Int,
        kind: BackupOp,
        result: Result<T>,
        formatVersion: Int?,
        done: (T) -> BackupUiState,
    ) {
        currentCoroutineContext().ensureActive()
        if (op != generation) return
        throttle.stop()
        result.onFailure { telemetry.report(kind, it, formatVersion) }
        _state.value = result.fold(done) { BackupUiState.Failed.of(it) }
    }

    /** A preview on screen: what it holds open, and where the officer's answer goes (null: cancelled). */
    private class PendingRestore(
        val picked: String,
        val prepared: PreparedRestore,
        val answer: CompletableDeferred<RestoreMode?>,
    )
}
