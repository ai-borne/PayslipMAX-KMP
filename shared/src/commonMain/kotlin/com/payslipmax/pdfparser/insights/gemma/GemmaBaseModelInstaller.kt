package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.flow.StateFlow

/**
 * Lifecycle of the store-delivered Tier 6 base model install, reported by the platform's asset
 * delivery mechanism (Play Asset Delivery on Android, Background Assets on iOS).
 */
sealed class BaseModelInstallState {
    object NotStarted : BaseModelInstallState()

    data class Downloading(val progress: Float) : BaseModelInstallState()

    /** Android's cellular-consent dialog (or an equivalent gate) needs a user response. */
    object NeedsUserConfirmation : BaseModelInstallState()

    data class Installed(val path: String) : BaseModelInstallState()

    data class Failed(val message: String) : BaseModelInstallState()
}

/**
 * Triggers and reports on the platform-native install of the Tier 6 Gemma base model. Android's
 * actual wraps `AssetPackManager.requestFetch()` (Phase 3); iOS's actual is a progress/completion
 * reporter only, since Background Assets schedules the download autonomously (Phase 4) — [install]
 * there is a no-op and state changes arrive via the registered Swift bridge instead.
 */
interface GemmaBaseModelInstaller {
    val state: StateFlow<BaseModelInstallState>

    suspend fun install()
}

expect fun provideGemmaBaseModelInstaller(): GemmaBaseModelInstaller
