package com.payslipmax.pdfparser.debugseed

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the developer sees: the outcome of the last action, and whether one is running. */
data class DebugSeedUiState(val message: String? = null, val busy: Boolean = false)

/** Runs one explicit developer action at a time; nothing here ever runs on its own. */
class DebugSeedViewModel(
    private val seeder: DebugSeeder,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(DebugSeedUiState())
    val uiState: StateFlow<DebugSeedUiState> = _uiState

    fun seed(step: SeedStep) =
        launch {
            when (val result = seeder.apply(step)) {
                is SeedResult.Seeded -> DebugSeedStrings.seeded(result.months)
                is SeedResult.Collision -> DebugSeedStrings.collision(result.months)
                is SeedResult.MissingPrerequisite -> DebugSeedStrings.missingPrerequisite(result.step)
            }
        }

    fun remove() = launch { DebugSeedStrings.removed(seeder.remove()) }

    fun dispose() = scope.cancel()

    private fun launch(action: suspend () -> String) {
        if (_uiState.value.busy) return
        _uiState.value = _uiState.value.copy(busy = true)
        scope.launch {
            val message = runCatching { action() }.getOrElse { "Failed: ${it.message}" }
            _uiState.value = DebugSeedUiState(message = message, busy = false)
        }
    }
}
