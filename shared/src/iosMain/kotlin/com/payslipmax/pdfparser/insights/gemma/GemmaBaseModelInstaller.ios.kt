package com.payslipmax.pdfparser.insights.gemma

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Background Assets-backed installer for the Tier 6 base model. Unlike Android's Play Asset
 * Delivery (which the app itself triggers via `requestFetch()`), Background Assets schedules its
 * download autonomously — device charging, on Wi-Fi, not in Low Power Mode — so [install] here is
 * not a trigger; the OS decides when its extension runs, not this call.
 *
 * Progress/completion instead arrive via [progressReporter]/[completionReporter]: this instance
 * registers its own reactive closures into those companion-object slots at construction, and the
 * still-to-be-written `GemmaBackgroundAssetsBridge.swift` — registered once from `iOSApp.swift`'s
 * `AppDelegate.didFinishLaunchingWithOptions`, the same call-site pattern as
 * `GemmaInferenceBridge.register()` — forwards the extension's real progress/completion events into
 * them. That Swift-side half (new Xcode target, App Group entitlement, Info.plist keys) is Phase
 * 4's remaining, Xcode-only work, blocked on Apple Developer Program enrollment — everything in
 * this file is pure Kotlin/Native and needs none of that to compile, test, or run.
 */
class IosGemmaBaseModelInstaller : GemmaBaseModelInstaller {
    private val _state = MutableStateFlow<BaseModelInstallState>(BaseModelInstallState.NotStarted)
    override val state: StateFlow<BaseModelInstallState> = _state.asStateFlow()

    init {
        progressReporter = { bytesDownloaded, totalBytes ->
            _state.value =
                BaseModelInstallState.Downloading(
                    if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else 0f,
                )
        }
        completionReporter = { success, errorMessage ->
            _state.value =
                if (success) {
                    BaseModelInstallState.Installed(resolveInstalledGemmaModelPath() ?: "")
                } else {
                    BaseModelInstallState.Failed(errorMessage ?: "Background Assets download failed")
                }
        }
    }

    override suspend fun install() {
        // Nothing to trigger — Background Assets schedules itself. If a previous launch's download
        // already completed (e.g. this is a re-verify-on-init call, not the first-ever launch),
        // reflect that immediately rather than sitting in NotStarted until the next OS-scheduled run.
        resolveInstalledGemmaModelPath()?.let { path ->
            _state.value = BaseModelInstallState.Installed(path)
        }
    }

    companion object {
        /** Invoked by GemmaBackgroundAssetsBridge.swift with (bytesDownloaded, totalBytes). */
        var progressReporter: ((Long, Long) -> Unit)? = null

        /** Invoked by GemmaBackgroundAssetsBridge.swift with (success, errorMessage). */
        var completionReporter: ((Boolean, String?) -> Unit)? = null
    }
}

actual fun provideGemmaBaseModelInstaller(): GemmaBaseModelInstaller = IosGemmaBaseModelInstaller()
