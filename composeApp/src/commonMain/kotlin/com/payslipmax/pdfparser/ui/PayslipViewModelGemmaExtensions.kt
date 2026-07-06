package com.payslipmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.insights.gemma.BaseModelInstallState
import com.payslipmax.pdfparser.insights.gemma.GemmaBaseModelInstaller
import com.payslipmax.pdfparser.insights.gemma.GemmaModelStorageManager
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sets which source (local Gemma vs. cloud Gemini) [FinancialIntelligenceRepository]'s narrative
 * insight generation reads from. The Tier 6 base model itself is downloaded unconditionally via
 * [PayslipViewModel]'s `GemmaBaseModelInstaller` trigger — this toggle no longer gates or triggers
 * that download.
 */
fun PayslipViewModel.setLocalAiEnabled(enabled: Boolean) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.payslipmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(useLocalAi = enabled))
    }
}

/**
 * Tier 6's base model is a mandatory, free-for-all background install, decoupled from the
 * "Use Local Gemma AI Model" toggle — it fires on every launch regardless of whether the user ever
 * touches that setting. Re-checks [GemmaModelStorageManager.verifyModelFile] on every init (not
 * just first-ever launch): a previously-installed model that's missing or fails validation (OS
 * storage cleanup, user clears app storage, partial delivery) is treated exactly like "not yet
 * installed" and re-triggers [GemmaBaseModelInstaller.install], closing the gap where a
 * corrupted/cleared model would otherwise silently leave Tier 6 dead forever.
 */
internal fun PayslipViewModel.installGemmaBaseModel() {
    viewModelScope.launch {
        gemmaBaseModelInstaller.state.collect { installState ->
            _uiState.update { it.applyInstallState(installState) }
        }
    }
    viewModelScope.launch {
        val alreadyReady = gemmaModelStorage.verifyModelFile(gemmaModelStorage.getRecommendedModelFileName()).isReady
        if (!alreadyReady) {
            gemmaBaseModelInstaller.install()
        }
    }
}

private fun PayslipUiState.applyInstallState(installState: BaseModelInstallState): PayslipUiState =
    when (installState) {
        is BaseModelInstallState.NotStarted ->
            copy(isDownloadingModel = false)
        is BaseModelInstallState.Downloading ->
            copy(isDownloadingModel = true, modelDownloadProgress = installState.progress, modelDownloadError = null)
        is BaseModelInstallState.NeedsUserConfirmation ->
            copy(isDownloadingModel = false)
        is BaseModelInstallState.Installed ->
            copy(isDownloadingModel = false, modelDownloadProgress = 1f, modelDownloadError = null)
        is BaseModelInstallState.Failed ->
            copy(isDownloadingModel = false, modelDownloadError = installState.message)
    }
