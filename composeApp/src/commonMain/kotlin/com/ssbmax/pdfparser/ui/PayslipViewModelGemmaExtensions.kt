package com.ssbmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.ssbmax.pdfparser.insights.gemma.DownloadStatus
import com.ssbmax.pdfparser.insights.gemma.GemmaModelDownloadManager
import com.ssbmax.pdfparser.insights.gemma.GemmaModelStorageManager
import com.ssbmax.pdfparser.insights.gemma.GemmaModelVersionManager
import com.ssbmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Interim shared access key for the manifest endpoint (Decision 3). Provisioning the real
// GEMMA_CACHE_KEY secret is an ops step tracked in the Phase 0 handoff; until it is wired the
// manifest fetch fails closed (403 → friendly error), which is the intended dev-mode behavior.
private val GEMMA_CACHE_KEY: String? = null

/**
 * Turns local (offline) AI on/off. When enabling, runs the version-aware, dual-slot download:
 * fetch the manifest (SSOT of the current version) → skip if the active slot already matches →
 * otherwise download the new version into the **staging** slot → verify its SHA-256 → only then
 * atomically promote it to active. A failed download or checksum leaves the existing active slot
 * untouched, so enabling can never take an already-working model offline.
 */
fun PayslipViewModel.setLocalAiEnabled(
    enabled: Boolean,
    storage: GemmaModelStorageManager = GemmaModelStorageManager(),
    versionManager: GemmaModelVersionManager = GemmaModelVersionManager(interimKey = GEMMA_CACHE_KEY),
    downloadManager: GemmaModelDownloadManager = GemmaModelDownloadManager(),
) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(useLocalAi = enabled))
        if (!enabled) return@launch

        _uiState.update { it.copy(isDownloadingModel = true, modelDownloadError = null) }

        // 1. Fetch the manifest — the single source of truth for what version is current.
        val manifest =
            versionManager.fetchManifest().getOrElse {
                _uiState.update { s -> s.copy(isDownloadingModel = false, modelDownloadError = AppStrings.modelManifestUnavailable) }
                return@launch
            }

        // Surface the Gemma Terms-of-Use notice as soon as the manifest is known — before/at
        // download — so the license's redistribution-notice requirement is met on every path below.
        _uiState.update { it.copy(modelDownloadNotice = manifest.noticeText) }

        // 2. Already on this version with the active slot present? Nothing to download.
        val activeReady = storage.verifyModelFile(storage.getRecommendedModelFileName()).isReady
        if (storage.getActiveVersion() == manifest.version && activeReady) {
            _uiState.update { it.copy(isDownloadingModel = false, modelDownloadProgress = 1f, modelDownloadError = null) }
            return@launch
        }

        // 3. Download the new version into the staging slot (active slot untouched meanwhile).
        val headers =
            if (GEMMA_CACHE_KEY != null) mapOf(GemmaModelVersionManager.INTERIM_KEY_HEADER to GEMMA_CACHE_KEY) else emptyMap()
        var downloadFailed = false
        downloadManager.downloadModel(manifest.url, storage.stagingSlotFileName(), headers).collect { status ->
            when (status) {
                is DownloadStatus.Idle -> {}
                is DownloadStatus.Downloading ->
                    _uiState.update { it.copy(isDownloadingModel = true, modelDownloadProgress = status.progress, modelDownloadError = null) }
                is DownloadStatus.Success -> {}
                is DownloadStatus.Error -> {
                    downloadFailed = true
                    _uiState.update { it.copy(isDownloadingModel = false, modelDownloadError = status.message) }
                }
            }
        }
        if (downloadFailed) return@launch

        // 4. Verify checksum, then atomically promote. On mismatch, discard staging and keep active.
        val promoted =
            withContext(Dispatchers.IO) {
                if (!storage.verifyStagingChecksum(manifest.sha256)) {
                    storage.discardStaging()
                    false
                } else {
                    storage.promoteStagingToActive(manifest.version)
                }
            }

        _uiState.update {
            if (promoted) {
                it.copy(isDownloadingModel = false, modelDownloadProgress = 1f, modelDownloadError = null)
            } else {
                it.copy(isDownloadingModel = false, modelDownloadError = AppStrings.modelVerificationFailed)
            }
        }
    }
}
