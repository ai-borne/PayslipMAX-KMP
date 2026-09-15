import Foundation
import composeApp

/// Bridges Apple's On-Demand Resources (ODR) fetch of the Tier 6 Gemma model to Kotlin
/// `IosGemmaBaseModelInstaller` companion slots.
///
/// Kotlin/Native `IosGemmaBaseModelInstaller.install()` invokes `installTrigger` to start the
/// fetch; this bridge reports progress/completion back via `progressReporter`/`completionReporter`.
/// The resource tag here must match exactly what's assigned to `gemma-active.litertlm` in Xcode's
/// asset catalog (On Demand Resource Tags), set to the "On Demand" download policy — not
/// "Prefetched: Install", which would bundle it into the initial app download instead of fetching
/// it later.
final class GemmaOnDemandResourceBridge {
    static let shared = GemmaOnDemandResourceBridge()

    /// Must be retained for as long as the app wants the fetched resource to stay on disk — ODR
    /// resources can be purged by the OS once no live `NSBundleResourceRequest` holds access to
    /// them. Held for the process lifetime once a fetch succeeds.
    private var activeRequest: NSBundleResourceRequest?
    private var progressObservation: NSKeyValueObservation?

    private static let tag = "GemmaModel"

    private init() {}

    /// Registers the bridge on app startup. Call from AppDelegate.didFinishLaunchingWithOptions.
    static func register() {
        IosGemmaBaseModelInstaller.companion.installTrigger = {
            GemmaOnDemandResourceBridge.shared.beginFetch()
        }
    }

    private func beginFetch() {
        let request = NSBundleResourceRequest(tags: [Self.tag])
        activeRequest = request

        progressObservation = Self.observeDownloadProgress(request.progress) { [weak self] bytesDownloaded, totalBytes in
            self?.reportProgress(bytesDownloaded: bytesDownloaded, totalBytes: totalBytes)
        }

        request.beginAccessingResources { [weak self] error in
            DispatchQueue.main.async {
                self?.progressObservation = nil
                if let error {
                    self?.reportCompletion(success: false, errorMessage: error.localizedDescription)
                    self?.activeRequest = nil
                } else {
                    self?.reportCompletion(success: true)
                }
            }
        }
    }

    /// Extracted for testability (see `GemmaOnDemandResourceBridgeProgressTests`) — this is the
    /// exact logic that regressed in commit `b518cb4` (shipped in iOS v1.2.1). The ~584MB GemmaModel
    /// ODR fetch starts with `request.progress` indeterminate (totalUnitCount == -1) because the
    /// resource size isn't known yet, and only becomes determinate once ODR reports it. The broken
    /// code checked `totalUnitCount > 0` once at observation setup time and skipped installing any
    /// observer at all when it was still indeterminate at that instant — permanently missing the
    /// later transition to determinate, leaving the UI stuck at "not started" for the whole
    /// download. Always observe fractionCompleted unconditionally instead: Foundation only starts
    /// reporting meaningful values for it once the progress is determinate, but the observation
    /// stays installed across that transition, matching the last known-working (v1.2) behavior.
    static func observeDownloadProgress(
        _ progress: Progress,
        onProgress: @escaping (Int64, Int64) -> Void
    ) -> NSKeyValueObservation {
        progress.observe(\.fractionCompleted) { progress, _ in
            let total: Int64 = 1000
            let done = Int64(progress.fractionCompleted * Double(total))
            onProgress(done, total)
        }
    }

    private func reportProgress(bytesDownloaded: Int64, totalBytes: Int64) {
        DispatchQueue.main.async {
            IosGemmaBaseModelInstaller.companion.progressReporter?(
                KotlinLong(value: bytesDownloaded),
                KotlinLong(value: totalBytes)
            )
        }
    }

    private func reportCompletion(success: Bool, errorMessage: String? = nil) {
        DispatchQueue.main.async {
            IosGemmaBaseModelInstaller.companion.completionReporter?(
                KotlinBoolean(bool: success),
                errorMessage
            )
        }
    }
}
