package com.ssbmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.ssbmax.pdfparser.insights.gemma.DownloadStatus
import com.ssbmax.pdfparser.insights.gemma.GemmaModelDownloadManager
import com.ssbmax.pdfparser.insights.gemma.GemmaModelStorageManager
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun PayslipViewModel.setLocalAiEnabled(enabled: Boolean) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(useLocalAi = enabled))

        if (enabled) {
            val storageManager = GemmaModelStorageManager()
            val recommendedFile = storageManager.getRecommendedModelFileName()
            val verification = storageManager.verifyModelFile(recommendedFile)

            if (verification.isReady) {
                _uiState.update { it.copy(isDownloadingModel = false, modelDownloadError = null) }
                return@launch
            }

            val downloadManager = GemmaModelDownloadManager()
            val modelUrl = "https://huggingface.co/google/gemma-3-1b-it-gguf/resolve/main/gemma-3-1b-it-int4.task"

            downloadManager.downloadModel(modelUrl, recommendedFile).collect { status ->
                when (status) {
                    is DownloadStatus.Idle -> {
                        _uiState.update { it.copy(isDownloadingModel = false) }
                    }
                    is DownloadStatus.Downloading -> {
                        _uiState.update { it.copy(isDownloadingModel = true, modelDownloadProgress = status.progress, modelDownloadError = null) }
                    }
                    is DownloadStatus.Success -> {
                        _uiState.update { it.copy(isDownloadingModel = false, modelDownloadProgress = 1f, modelDownloadError = null) }
                    }
                    is DownloadStatus.Error -> {
                        val friendlyMessage =
                            if (status.message.contains("401")) {
                                "Hugging Face Auth Required (Gated Model)."
                            } else {
                                status.message
                            }
                        _uiState.update { it.copy(isDownloadingModel = false, modelDownloadError = friendlyMessage) }
                    }
                }
            }
        }
    }
}
