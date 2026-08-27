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
 * Persists the user's choice to enable or disable telemetry collection.
 */
fun PayslipViewModel.setTelemetryEnabled(enabled: Boolean) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.payslipmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(isTelemetryEnabled = enabled))
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
            gemmaInstallTelemetry.trackInstallState(installState)
            _uiState.update { it.applyInstallState(installState) }
        }
    }
    viewModelScope.launch {
        val installedPath = com.payslipmax.pdfparser.insights.gemma.resolveInstalledGemmaModelPath()
        val alreadyReady =
            if (installedPath != null) {
                gemmaModelStorage.verifyModelFile(installedPath).isReady
            } else {
                gemmaModelStorage.verifyModelFile(gemmaModelStorage.getRecommendedModelFileName()).isReady
            }
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

internal fun PayslipViewModel.checkGemmaSupport() {
    try {
        val capManager = com.payslipmax.pdfparser.insights.gemma.DeviceCapabilityManager()
        val status = capManager.checkGemmaSupport()
        val (supported, reason) =
            when (status) {
                is com.payslipmax.pdfparser.insights.gemma.GemmaSupportStatus.Supported -> true to null
                is com.payslipmax.pdfparser.insights.gemma.GemmaSupportStatus.InsufficientRam -> false to "Requires device with 4GB RAM"
                is com.payslipmax.pdfparser.insights.gemma.GemmaSupportStatus.InsufficientStorage -> false to "Requires 1.5GB free storage"
                is com.payslipmax.pdfparser.insights.gemma.GemmaSupportStatus.UnsupportedArchitecture -> false to status.reason
            }
        _uiState.update { it.copy(isGemmaSupported = supported, gemmaSupportReason = reason) }
    } catch (e: Throwable) {
        _uiState.update { it.copy(isGemmaSupported = true, gemmaSupportReason = null) }
    }
}

internal fun PayslipViewModel.observeSettings() {
    var isFirstSettingsLoad = true
    var previousPremiumEnabled = false
    viewModelScope.launch {
        repository.getSettingsFlow().collect { settings ->
            val isPremium = settings?.isPremiumEnabled ?: false
            val isTelemetry = settings?.isTelemetryEnabled ?: true
            gemmaInstallTelemetry.setTelemetryEnabled(isTelemetry)
            _uiState.update { state ->
                val isLocked =
                    if (isFirstSettingsLoad) {
                        isFirstSettingsLoad = false
                        settings?.isLockEnabled ?: false
                    } else {
                        state.isAppLocked && (settings?.isLockEnabled ?: false)
                    }
                state.copy(
                    isPremiumEnabled = isPremium,
                    appTheme = settings?.appTheme ?: "system",
                    isLockEnabled = settings?.isLockEnabled ?: false,
                    appPinHash = settings?.appPinHash ?: "",
                    profileName = settings?.profileName ?: "",
                    profileCdaNumber = settings?.profileCdaNumber ?: "",
                    profilePanNumber = settings?.profilePanNumber ?: "",
                    isAppLocked = isLocked,
                    useLocalAi = settings?.useLocalAi ?: false,
                    isTelemetryEnabled = isTelemetry,
                )
            }
            previousPremiumEnabled = isPremium
        }
    }
}
